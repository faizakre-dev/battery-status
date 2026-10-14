package com.faizakram.batterystatus.core.health

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatteryHealthAssessment
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason

/**
 * Maps the platform's [BatteryHealth] plus the current temperature into a
 * single coarse [HealthLevel]. Pure and stateless so it can be unit-tested and
 * reused by the tracker/estimators later.
 */
object BatteryHealthEvaluator {

    private const val WARNING_TEMPERATURE_C = 40f
    private const val CRITICAL_TEMPERATURE_C = 45f
    private const val COLD_TEMPERATURE_C = 0f

    fun assess(snapshot: BatterySnapshot): BatteryHealthAssessment {
        val reasons = mutableListOf<HealthReason>()
        var level = HealthLevel.GOOD

        when (snapshot.health) {
            BatteryHealth.GOOD -> reasons += HealthReason.HEALTHY
            BatteryHealth.OVERHEAT -> {
                level = worse(level, HealthLevel.POOR)
                reasons += HealthReason.OVERHEAT
            }
            BatteryHealth.COLD -> {
                level = worse(level, HealthLevel.ATTENTION)
                reasons += HealthReason.COLD
            }
            BatteryHealth.DEAD -> {
                level = worse(level, HealthLevel.POOR)
                reasons += HealthReason.DEAD
            }
            BatteryHealth.OVER_VOLTAGE -> {
                level = worse(level, HealthLevel.POOR)
                reasons += HealthReason.OVER_VOLTAGE
            }
            BatteryHealth.UNSPECIFIED_FAILURE -> {
                level = worse(level, HealthLevel.POOR)
                reasons += HealthReason.FAILURE
            }
            BatteryHealth.UNKNOWN -> {
                level = worse(level, HealthLevel.UNKNOWN)
                reasons += HealthReason.UNKNOWN
            }
        }

        val alreadyOverheating = snapshot.health == BatteryHealth.OVERHEAT
        val alreadyCold = snapshot.health == BatteryHealth.COLD
        snapshot.temperatureCelsius?.let { temperature ->
            when {
                temperature >= CRITICAL_TEMPERATURE_C -> {
                    level = worse(level, HealthLevel.POOR)
                    if (!alreadyOverheating) reasons += HealthReason.TEMPERATURE_HIGH
                }
                temperature >= WARNING_TEMPERATURE_C -> {
                    level = worse(level, HealthLevel.ATTENTION)
                    if (!alreadyOverheating) reasons += HealthReason.TEMPERATURE_HIGH
                }
                temperature <= COLD_TEMPERATURE_C -> {
                    level = worse(level, HealthLevel.ATTENTION)
                    if (!alreadyCold) reasons += HealthReason.TEMPERATURE_LOW
                }
            }
        }

        if (reasons.isEmpty()) reasons += HealthReason.HEALTHY
        return BatteryHealthAssessment(level = level, reasons = reasons.distinct())
    }

    private fun worse(current: HealthLevel, candidate: HealthLevel): HealthLevel =
        if (rank(candidate) > rank(current)) candidate else current

    private fun rank(level: HealthLevel): Int = when (level) {
        HealthLevel.GOOD -> 0
        HealthLevel.UNKNOWN -> 1
        HealthLevel.ATTENTION -> 2
        HealthLevel.POOR -> 3
    }
}
