package com.faizakram.batterystatus.core.quirks

import com.faizakram.batterystatus.core.model.BatteryStatus
import kotlin.math.abs

/**
 * Normalizes `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` readings, whose sign
 * and plausibility vary by OEM.
 *
 * Canonical convention returned by [normalize]: **positive means current is
 * flowing into the battery (charging), negative means it is flowing out
 * (discharging)**. Android's docs only guarantee the magnitude, so the sign is
 * reconciled against the reported [BatteryStatus] at runtime.
 */
object CurrentNormalizer {

    fun normalize(
        rawMicroAmps: Int?,
        status: BatteryStatus,
        maxPlausibleMicroAmps: Int = DeviceQuirks.DEFAULT_MAX_PLAUSIBLE_CURRENT_UA,
    ): Int? {
        if (rawMicroAmps == null) return null
        if (rawMicroAmps == 0) return 0
        if (abs(rawMicroAmps) > maxPlausibleMicroAmps) return null

        val charging = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL
        return when {
            charging && rawMicroAmps < 0 -> -rawMicroAmps
            status == BatteryStatus.DISCHARGING && rawMicroAmps > 0 -> -rawMicroAmps
            else -> rawMicroAmps
        }
    }
}
