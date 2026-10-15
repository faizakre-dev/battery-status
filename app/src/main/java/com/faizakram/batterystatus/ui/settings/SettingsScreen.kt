package com.faizakram.batterystatus.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizakram.batterystatus.R
import com.faizakram.batterystatus.core.update.ReleaseInfo
import com.faizakram.batterystatus.core.update.UpdateError
import com.faizakram.batterystatus.core.update.UpdateState
import com.faizakram.batterystatus.ui.theme.BatteryStatusTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    SettingsContent(
        state = state,
        onCheck = viewModel::checkForUpdates,
        onDownload = viewModel::download,
        onInstall = viewModel::install,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        AboutCard(state.currentVersion)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                UpdateStatus(state)

                (state.update as? UpdateState.Available)?.release?.let { release ->
                    ReleaseNotes(release)
                }

                UpdateActions(
                    state = state,
                    onCheck = onCheck,
                    onDownload = onDownload,
                    onInstall = onInstall,
                )
            }
        }
    }
}

@Composable
private fun AboutCard(currentVersion: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_about_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.update_version_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentVersion,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun UpdateStatus(state: SettingsUiState) {
    when (val update = state.update) {
        UpdateState.Idle, UpdateState.Checking -> StatusRow(
            text = stringResource(
                if (update is UpdateState.Checking) R.string.update_checking
                else R.string.update_idle,
            ),
            indeterminate = update is UpdateState.Checking,
        )

        UpdateState.UpToDate -> Text(
            text = stringResource(R.string.update_up_to_date, state.currentVersion),
            style = MaterialTheme.typography.bodyLarge,
        )

        is UpdateState.Available -> Text(
            text = stringResource(R.string.update_available, update.release.version),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        is UpdateState.Downloading -> DownloadStatus(update)

        is UpdateState.ReadyToInstall -> {
            Text(
                text = stringResource(R.string.update_ready, update.release.version),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.needsInstallPermission) {
                Text(
                    text = stringResource(R.string.update_permission_needed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is UpdateState.Failed -> Text(
            text = stringResource(updateErrorRes(update.error)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StatusRow(text: String, indeterminate: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indeterminate) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadStatus(update: UpdateState.Downloading) {
    val percent = update.progressPercent

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (percent == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.update_downloading_unknown),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.update_downloading, percent),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReleaseNotes(release: ReleaseInfo) {
    if (release.notes.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.update_notes, release.version),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = release.notes,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun UpdateActions(
    state: SettingsUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (val update = state.update) {
        is UpdateState.Available -> Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.update_download))
        }

        is UpdateState.Failed -> if (update.release != null) {
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.update_download))
            }
        }

        is UpdateState.ReadyToInstall -> Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.update_install))
        }

        else -> Unit
    }

    val update = state.update
    if (update !is UpdateState.Checking && update !is UpdateState.Downloading) {
        OutlinedButton(
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.update_check))
        }
    }
}

@StringRes
private fun updateErrorRes(error: UpdateError): Int = when (error) {
    UpdateError.NETWORK -> R.string.update_error_network
    UpdateError.NOT_FOUND -> R.string.update_error_not_found
    UpdateError.PARSE -> R.string.update_error_parse
    UpdateError.NO_APK -> R.string.update_error_apk
    UpdateError.STORAGE -> R.string.update_error_storage
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentAvailablePreview() {
    BatteryStatusTheme {
        SettingsContent(
            state = SettingsUiState(
                currentVersion = "0.3.0",
                update = UpdateState.Available(
                    ReleaseInfo(
                        tagName = "v0.4.0",
                        version = "0.4.0",
                        name = "v0.4.0",
                        notes = "Adds an in-app updater and fixes chart labels.",
                        apkUrl = "https://example.com/app-debug.apk",
                        publishedAt = "2026-10-15T00:00:00Z",
                    ),
                ),
            ),
            onCheck = {},
            onDownload = {},
            onInstall = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentDownloadingPreview() {
    BatteryStatusTheme {
        SettingsContent(
            state = SettingsUiState(
                currentVersion = "0.3.0",
                update = UpdateState.Downloading(
                    release = ReleaseInfo(
                        tagName = "v0.4.0",
                        version = "0.4.0",
                        name = "v0.4.0",
                        notes = "",
                        apkUrl = "https://example.com/app-debug.apk",
                        publishedAt = "2026-10-15T00:00:00Z",
                    ),
                    progressPercent = 42,
                ),
            ),
            onCheck = {},
            onDownload = {},
            onInstall = {},
        )
    }
}
