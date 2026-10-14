package com.faizakram.batterystatus.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.isCharging
import com.faizakram.batterystatus.core.quirks.DeviceQuirks
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryData
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryReader
import com.faizakram.batterystatus.data.repo.BatteryRepository
import com.faizakram.batterystatus.domain.stats.ChargeSample
import com.faizakram.batterystatus.domain.stats.TimeToFullEstimator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    batteryInfoSource: BatteryInfoSource,
    repository: BatteryRepository,
    sysfsBatteryReader: SysfsBatteryReader,
    private val deviceQuirks: DeviceQuirks,
) : ViewModel() {

    private val timeEstimator = TimeToFullEstimator()
    private val sysfsData = MutableStateFlow(SysfsBatteryData.NONE)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            sysfsData.value = sysfsBatteryReader.read()
        }
    }

    val uiState: StateFlow<DashboardUiState?> = combine(
        batteryInfoSource.snapshots,
        repository.observeEstimates().map { it ?: BatteryEstimates.EMPTY },
        sysfsData,
    ) { snapshot, estimates, sysfs ->
        val rate = timeEstimator.onSample(
            ChargeSample(
                timestampMillis = snapshot.timestampMillis,
                chargeCounterUah = snapshot.chargeCounterUah,
                isCharging = snapshot.isCharging,
            ),
        )
        snapshot.toDashboardUiState(estimates, sysfs, rate, deviceQuirks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}
