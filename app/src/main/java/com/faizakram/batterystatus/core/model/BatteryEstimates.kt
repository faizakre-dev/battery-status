package com.faizakram.batterystatus.core.model

/**
 * Accumulated, app-owned battery estimates persisted in Room (single row).
 *
 * - [designCapacityUah]: design capacity, from sysfs or a lifetime-peak fallback.
 * - [lifetimePeakUah]: largest observed charge counter, used as a health proxy.
 * - [accumulatedCycleFraction]: fractional charge cycles summed over sessions.
 * - [sysfsCycleCount]: real kernel cycle count when readable, else `null`.
 */
data class BatteryEstimates(
    val designCapacityUah: Int?,
    val lifetimePeakUah: Int?,
    val accumulatedCycleFraction: Double,
    val sysfsCycleCount: Int?,
    val lastUpdatedMillis: Long,
) {
    companion object {
        val EMPTY = BatteryEstimates(
            designCapacityUah = null,
            lifetimePeakUah = null,
            accumulatedCycleFraction = 0.0,
            sysfsCycleCount = null,
            lastUpdatedMillis = 0L,
        )
    }
}
