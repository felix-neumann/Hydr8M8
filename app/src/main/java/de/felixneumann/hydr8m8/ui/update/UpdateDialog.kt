package de.felixneumann.hydr8m8.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.update.ReleaseInfo
import de.felixneumann.hydr8m8.update.UpdateFailure
import de.felixneumann.hydr8m8.update.UpdateState

/** Popup for the update flow: offer, download progress, install hand-off, result and errors. */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: (ReleaseInfo) -> Unit,
    onInstall: () -> Unit,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissButton = @Composable { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } }
    when (state) {
        UpdateState.Idle -> {
            Unit
        }

        UpdateState.Checking -> {
            ProgressDialog(
                stringResource(R.string.update_checking),
                progress = null,
                detail = null,
                onDismiss,
                modifier,
            )
        }

        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier,
                title = { Text(stringResource(R.string.update_up_to_date)) },
                text = { Text(stringResource(R.string.update_up_to_date_text, state.installed.toString())) },
                confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
            )
        }

        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier,
                title = { Text(stringResource(R.string.update_available_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(
                                R.string.update_available_text,
                                state.release.version.toString(),
                                state.installed.toString(),
                                Format.megabytes(state.release.apkSizeBytes),
                            ),
                        )
                        if (state.release.notes.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                state.release.notes,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDownload(state.release)
                    }) { Text(stringResource(R.string.update_install)) }
                },
                dismissButton = dismissButton,
            )
        }

        is UpdateState.Downloading -> {
            ProgressDialog(
                stringResource(R.string.update_downloading, state.release.version.toString()),
                progress = state.progress,
                detail =
                    stringResource(
                        R.string.update_progress,
                        Format.megabytes(state.downloadedBytes),
                        Format.megabytes(state.totalBytes),
                        (state.progress * 100).toInt(),
                    ),
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier,
                title = { Text(stringResource(R.string.update_ready_title, state.release.version.toString())) },
                text = { Text(stringResource(R.string.update_ready_text)) },
                confirmButton = {
                    TextButton(onClick = onInstall) { Text(stringResource(R.string.update_open_downloads)) }
                },
                dismissButton = dismissButton,
            )
        }

        is UpdateState.Failed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier,
                title = { Text(stringResource(R.string.update_failed)) },
                text = { Text(stringResource(failureText(state.failure))) },
                confirmButton = {
                    TextButton(onClick = { state.release?.let(onDownload) ?: onRetryCheck() }) {
                        Text(stringResource(R.string.update_retry))
                    }
                },
                dismissButton = dismissButton,
            )
        }
    }
}

private fun failureText(failure: UpdateFailure): Int =
    when (failure) {
        UpdateFailure.NETWORK -> R.string.update_error_network
        UpdateFailure.UNTRUSTED_SOURCE -> R.string.update_error_untrusted
        UpdateFailure.DOWNLOAD -> R.string.update_error_download
        UpdateFailure.WRONG_SIGNATURE -> R.string.update_error_signature
        UpdateFailure.INVALID_FILE -> R.string.update_error_invalid
    }

/** Dialog with a determinate ([progress] in 0..1) or indeterminate (null) progress bar. */
@Composable
private fun ProgressDialog(
    title: String,
    progress: Float?,
    detail: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column {
                if (progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                if (detail != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_hide)) } },
    )
}
