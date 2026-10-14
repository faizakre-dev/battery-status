package com.faizakram.batterystatus.ui.dashboard

import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.health.BatteryHealthEvaluator
import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason
import com.faizakram.batterystatus.core.model.PlugType
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryData
import com.faizakram.batterystatus.domain.cycle.CycleEstimator
import com.faizakram.batterystatus.domain.health.DesignCapacityResolver
import com.faizakram.batterystatus.domain.health.HealthCalculator
import com.faizakram.batterystatus.domain.stats.TimeToFullEstimator

/** Everything the dashboard needs, decoupled from the raw [BatterySnapshot]. */
data class DashboardUiState(
    val levelPercent: Int?,
    val status: BatteryStatus,
    val plugged: PlugType,
    val healthLevel: HealthLevel,
    val healthReasons: List<HealthReason>,
    val temperatureText: String?,
    val voltageText: String?,
    val currentText: String?,
    val technology: String?,
    val updatedAtMillis: Long,
    val estimatedCycles: Double?,
    val cyclesFromSystem: Boolean,
    val healthPercent: Int?,
    val healthPercentEstimated: Boolean,
    val timeToFullText: String?,
    val timeRemainingText: String?,
)

fun BatterySnapshot.toDashboardUiState(
    estimates: BatteryEstimates,
    sysfs: SysfsBatteryData,
    smoothedRateUaPerMs: Double?,
): DashboardUiState {
    val assessment = BatteryHealthEvaluator.assess(this)

    val designCapacityUah = DesignCapacityResolver.resolve(
        sysfsDesignUah = sysfs.chargeFullDesignUah,
        currentDesignUah = estimates.designCapacityUah,
        lifetimePeakUah = estimates.lifetimePeakUah,
    )
    val sysfsHealthPercent = HealthCalculator.healthPercent(
        fullChargeUah = sysfs.chargeFullUah,
        designCapacityUah = sysfs.chargeFullDesignUah,
    )
    val estimatedHealthPercent = HealthCalculator.healthPercent(
        fullChargeUah = estimates.lifetimePeakUah,
        designCapacityUah = designCapacityUah,
    )
    val healthPercent = sysfsHealthPercent ?: estimatedHealthPercent

    val sysfsCycleCount = sysfs.cycleCount ?: estimates.sysfsCycleCount
    val cycles = CycleEstimator.effectiveCycles(
        accumulatedCycleFraction = estimates.accumulatedCycleFraction,
        sysfsCycleCount = sysfsCycleCount,
    )

    val fullCapacityUah = sysfs.chargeFullUah ?: designCapacityUah

    return DashboardUiState(
        levelPercent = levelPercent,
        status = status,
        plugged = plugged,
        healthLevel = assessment.level,
        healthReasons = assessment.reasons,
        temperatureText = BatteryFormatters.temperature(temperatureCelsius),
        voltageText = BatteryFormatters.voltage(voltageMv),
        currentText = BatteryFormatters.currentMilliamps(currentNowUa),
        technology = technology,
        updatedAtMillis = timestampMillis,
        estimatedCycles = cycles,
        cyclesFromSystem = sysfsCycleCount != null,
        healthPercent = healthPercent,
        healthPercentEstimated = sysfsHealthPercent == null && estimatedHealthPercent != null,
        timeToFullText = BatteryFormatters.duration(
            TimeToFullEstimator.timeToFullMillis(fullCapacityUah, chargeCounterUah, smoothedRateUaPerMs),
        ),
        timeRemainingText = BatteryFormatters.duration(
            TimeToFullEstimator.remainingMillis(chargeCounterUah, smoothedRateUaPerMs),
        ),
    )
}
