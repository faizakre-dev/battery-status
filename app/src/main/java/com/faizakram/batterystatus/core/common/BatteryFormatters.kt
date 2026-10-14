package com.faizakram.batterystatus.core.common

import java.util.Locale

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
}
