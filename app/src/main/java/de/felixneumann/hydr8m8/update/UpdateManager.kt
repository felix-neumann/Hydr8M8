package de.felixneumann.hydr8m8.update

import de.felixneumann.hydr8m8.domain.AppVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why an update step failed; mapped to a localized message by the UI. */
enum class UpdateFailure { NETWORK, UNTRUSTED_SOURCE, DOWNLOAD, WRONG_SIGNATURE, INVALID_FILE }

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class UpToDate(
        val installed: AppVersion,
    ) : UpdateState

    data class Available(
        val release: ReleaseInfo,
        val installed: AppVersion,
    ) : UpdateState

    data class Downloading(
        val release: ReleaseInfo,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateState {
        val progress: Float get() =
            if (totalBytes >
                0
            ) {
                (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    /** Downloaded, verified and saved to Documents/Hydr8M8; the system installer takes over from the file manager. */
    data class ReadyToInstall(
        val release: ReleaseInfo,
    ) : UpdateState

    data class Failed(
        val failure: UpdateFailure,
        val release: ReleaseInfo? = null,
    ) : UpdateState
}

/**
 * Checks GitHub for a newer release, downloads it with progress and verifies it before handing it
 * to the system installer. [dialogVisible] drives the update popup: shown when an update is found,
 * for manual checks, and while downloading or failing.
 */
class UpdateManager(
    private val scope: CoroutineScope,
    private val installedVersionName: String,
    private val repo: String,
    private val releases: GitHubReleases,
    private val downloader: UpdateDownloader,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    private var autoChecked = false
    private var job: Job? = null

    private val installed: AppVersion get() = AppVersion.parse(installedVersionName) ?: AppVersion(0, 0, 0)

    /** Automatic check once per process start; only surfaces a popup if an update exists. */
    fun checkOnStart() {
        if (autoChecked) return
        autoChecked = true
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (job?.isActive == true) return
        job =
            scope.launch {
                _state.value = UpdateState.Checking
                val result =
                    runCatching { releases.latest() }.fold(
                        onSuccess = { release -> evaluate(release) },
                        onFailure = { UpdateState.Failed(UpdateFailure.NETWORK) },
                    )
                _state.value = result
                if (manual || result is UpdateState.Available) _dialogVisible.value = true
            }
    }

    private fun evaluate(release: ReleaseInfo): UpdateState =
        when {
            release.version <= installed -> {
                downloader.removePrevious() // the downloaded file is no longer needed
                UpdateState.UpToDate(installed)
            }

            !UpdateVerification.isTrustedAssetUrl(release.apkUrl, repo) -> {
                UpdateState.Failed(UpdateFailure.UNTRUSTED_SOURCE, release)
            }

            else -> {
                UpdateState.Available(release, installed)
            }
        }

    /** Downloads and verifies [release]; afterwards the user installs it via the system. */
    fun download(release: ReleaseInfo) {
        if (job?.isActive == true) return
        if (!UpdateVerification.isTrustedAssetUrl(release.apkUrl, repo)) {
            _state.value = UpdateState.Failed(UpdateFailure.UNTRUSTED_SOURCE, release)
            return
        }
        _dialogVisible.value = true
        job =
            scope.launch {
                _state.value = UpdateState.Downloading(release, 0, release.apkSizeBytes)
                val outcome =
                    runCatching {
                        downloader.download(release) { done, total ->
                            _state.value = UpdateState.Downloading(release, done, total)
                        }
                    }.getOrElse { DownloadOutcome.Failed }
                _state.value =
                    when (outcome) {
                        DownloadOutcome.Verified -> {
                            UpdateState.ReadyToInstall(release)
                        }

                        is DownloadOutcome.Failed -> {
                            UpdateState.Failed(UpdateFailure.DOWNLOAD, release)
                        }

                        is DownloadOutcome.Rejected -> {
                            UpdateState.Failed(
                                if (outcome.reason == UpdateVerification.Result.WrongSigner) {
                                    UpdateFailure.WRONG_SIGNATURE
                                } else {
                                    UpdateFailure.INVALID_FILE
                                },
                            )
                        }
                    }
                _dialogVisible.value = true
            }
    }

    /** Hands over to the system: opens the folder where the user taps the APK to install it. */
    fun install() {
        downloader.openFolder()
        _dialogVisible.value = false
    }

    fun dismiss() {
        _dialogVisible.value = false
        // A running download continues in the background (system notification).
        if (_state.value !is UpdateState.Downloading) job?.cancel()
    }
}
