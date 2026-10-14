package com.faizakram.batterystatus.core.model

/**
 * Coarse health verdict shown in the UI. Ordered from best ([GOOD]) to worst
 * ([POOR]); [UNKNOWN] sits between them because we simply have no reading.
 */
enum class HealthLevel {
    GOOD,
    UNKNOWN,
    ATTENTION,
    POOR,
}

/** Why a particular [HealthLevel] was chosen; drives the explanatory subtitle. */
enum class HealthReason {
    HEALTHY,
    OVERHEAT,
    COLD,
    DEAD,
    OVER_VOLTAGE,
    FAILURE,
    UNKNOWN,
    TEMPERATURE_HIGH,
    TEMPERATURE_LOW,
}

/** Result of evaluating a [BatterySnapshot]'s health signals. */
data class BatteryHealthAssessment(
    val level: HealthLevel,
    val reasons: List<HealthReason>,
)
