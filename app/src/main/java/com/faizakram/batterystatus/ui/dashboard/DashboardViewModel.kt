package com.faizakram.batterystatus.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    batteryInfoSource: BatteryInfoSource,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState?> = batteryInfoSource.snapshots
        .map { it.toDashboardUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
