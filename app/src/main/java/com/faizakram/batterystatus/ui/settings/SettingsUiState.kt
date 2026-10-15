package com.faizakram.batterystatus.ui.settings

import com.faizakram.batterystatus.core.update.UpdateState

/** Everything the Settings screen renders. */
data class SettingsUiState(
    /** Installed app version, shown in the About card. */
    val currentVersion: String,
    /** Progress of the GitHub-release update flow. */
    val update: UpdateState,
    /** True when the install prompt was blocked and the user must grant it. */
    val needsInstallPermission: Boolean = false,
)
