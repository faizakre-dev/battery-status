package com.faizakram.batterystatus.ui.dashboard

import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.health.BatteryHealthEvaluator
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason
import com.faizakram.batterystatus.core.model.PlugType

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
)

fun BatterySnapshot.toDashboardUiState(): DashboardUiState {
    val assessment = BatteryHealthEvaluator.assess(this)
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
    )
}
