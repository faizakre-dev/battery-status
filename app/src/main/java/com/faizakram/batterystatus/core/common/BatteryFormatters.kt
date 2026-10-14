package com.faizakram.batterystatus.core.common

import java.util.Locale
import kotlin.math.floor

/**
 * Pure value formatters shared by all screens. Uses [Locale.US] so the output
 * is stable regardless of the device locale (and deterministic in tests).
 */
object BatteryFormatters {

    fun levelPercent(percent: Int?): String =
        percent?.let { "$it%" } ?: "--"

    fun voltage(voltageMv: Int?): String? =
        voltageMv?.let { String.format(Locale.US, "%.2f V", it / 1000f) }

    fun currentMilliamps(currentUa: Int?): String? =
        currentUa?.let { String.format(Locale.US, "%.0f mA", it / 1000f) }

    fun temperature(celsius: Float?): String? =
        celsius?.let { String.format(Locale.US, "%.1f °C", it) }

    /** Whole charge cycles (fractional part dropped), e.g. `128`. */
    fun cycles(cycles: Double?): String? =
        cycles?.let { floor(it).toLong().toString() }

    /** Human-readable duration bucket, e.g. `1h 23m`, `45m`, `2h`. */
    fun duration(millis: Long?): String? {
        if (millis == null || millis <= 0) return null
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
