package com.faizakram.batterystatus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faizakram.batterystatus.core.update.UpdateManager
import com.faizakram.batterystatus.core.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateManager: UpdateManager,
) : ViewModel() {

    private val needsInstallPermission = MutableStateFlow(false)

    init {
        // Detect updates as soon as Settings is opened; the singleton only
        // auto-checks once per process, so tab switches don't re-hit the API.
        if (updateManager.state.value is UpdateState.Idle) {
            updateManager.checkForUpdates()
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        updateManager.state,
        needsInstallPermission,
    ) { update, permission ->
        SettingsUiState(
            currentVersion = updateManager.currentVersion,
            update = update,
            needsInstallPermission = permission,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            currentVersion = updateManager.currentVersion,
            update = UpdateState.Idle,
        ),
    )

    fun checkForUpdates() {
        needsInstallPermission.value = false
        updateManager.checkForUpdates()
    }

    fun download() = updateManager.downloadUpdate()

    fun install() {
        if (updateManager.canInstall()) {
            needsInstallPermission.value = false
            updateManager.installUpdate()
        } else {
            needsInstallPermission.value = true
            updateManager.openInstallPermissionSettings()
        }
    }

    fun onScreenResumed() {
        if (updateManager.canInstall()) needsInstallPermission.value = false
    }
}
