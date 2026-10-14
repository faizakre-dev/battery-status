package com.faizakram.batterystatus.domain.retention

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.HourlySnapshot

/**
 * Pure retention policy: raw snapshots are kept for [RAW_RETENTION_MILLIS] and
 * then compressed into one [HourlySnapshot] per [BUCKET_MILLIS] bucket.
 */
object SnapshotRetention {

    const val RAW_RETENTION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    const val BUCKET_MILLIS: Long = 60L * 60 * 1000

    /** Start timestamp of the hour bucket that [millis] falls into. */
    fun bucketStart(millis: Long): Long = millis - (millis % BUCKET_MILLIS)

    /**
     * Groups [snapshots] into hour buckets and summarizes each. Buckets are
     * returned in ascending start-time order. Input order does not matter.
     */
    fun aggregateHourly(snapshots: List<BatterySnapshot>): List<HourlySnapshot> =
        snapshots
            .groupBy { bucketStart(it.timestampMillis) }
            .toSortedMap()
            .map { (bucket, group) -> aggregate(bucket, group) }

    private fun aggregate(bucket: Long, group: List<BatterySnapshot>): HourlySnapshot {
        val levels = group.mapNotNull { it.levelPercent }
        val temperatures = group.mapNotNull { it.temperatureCelsius }
        return HourlySnapshot(
            bucketStartMillis = bucket,
            sampleCount = group.size,
            avgLevelPercent = levels.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            minLevelPercent = levels.minOrNull(),
            maxLevelPercent = levels.maxOrNull(),
            avgTemperatureCelsius = temperatures.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            minTemperatureCelsius = temperatures.minOrNull(),
            maxTemperatureCelsius = temperatures.maxOrNull(),
        )
    }
}
