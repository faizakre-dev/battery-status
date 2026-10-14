package com.faizakram.batterystatus.core.model

/**
 * A one-hour aggregate of raw [BatterySnapshot] rows, persisted after the raw
 * retention window so long-term history survives without unbounded growth.
 */
data class HourlySnapshot(
    val bucketStartMillis: Long,
    val sampleCount: Int,
    val avgLevelPercent: Float?,
    val minLevelPercent: Int?,
    val maxLevelPercent: Int?,
    val avgTemperatureCelsius: Float?,
    val minTemperatureCelsius: Float?,
    val maxTemperatureCelsius: Float?,
)
