package com.faizakram.batterystatus.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.BatterySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    batteryInfoSource: BatteryInfoSource,
) : ViewModel() {

    val snapshot: StateFlow<BatterySnapshot?> = batteryInfoSource.snapshots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
