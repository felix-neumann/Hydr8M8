package de.felixneumann.hydr8m8.update

import de.felixneumann.hydr8m8.domain.AppVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        val progress: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }

    data object Installing : UpdateState

    data class Failed(
        val message: String?,
        val release: ReleaseInfo? = null,
    ) : UpdateState
}

/**
 * Checks GitHub for a newer release, downloads it with progress and starts the installation.
 * [dialogVisible] drives the update popup: shown when an update is found, for manual checks and
 * while downloading or failing.
 */
class UpdateManager(
    private val scope: CoroutineScope,
    private val installedVersionName: String,
    private val releases: GitHubReleases,
    private val installer: UpdateInstaller,
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
                        onSuccess = { release ->
                            if (release.version > installed) {
                                UpdateState.Available(release, installed)
                            } else {
                                UpdateState.UpToDate(installed)
                            }
                        },
                        onFailure = { UpdateState.Failed(it.message) },
                    )
                _state.value = result
                if (manual || result is UpdateState.Available) _dialogVisible.value = true
            }
    }

    /** Downloads and installs [release]. Requires the "install unknown apps" permission. */
    fun downloadAndInstall(release: ReleaseInfo) {
        if (!installer.canInstall()) {
            installer.openInstallPermissionSettings()
            return
        }
        if (job?.isActive == true) return
        _dialogVisible.value = true
        job =
            scope.launch {
                runCatching {
                    _state.value = UpdateState.Downloading(release, 0, release.apkSizeBytes)
                    val apk =
                        installer.download(release.apkUrl, release.apkSizeBytes) { done, total ->
                            _state.value = UpdateState.Downloading(release, done, total)
                        }
                    _state.value = UpdateState.Installing
                    installer.install(apk)
                }.onFailure { _state.value = UpdateState.Failed(it.message, release) }
            }
    }

    fun onInstallFailed(message: String?) {
        _state.value = UpdateState.Failed(message)
        _dialogVisible.value = true
    }

    fun dismiss() {
        _dialogVisible.value = false
        if (_state.value !is UpdateState.Downloading && _state.value !is UpdateState.Installing) {
            job?.cancel()
        }
    }
}
