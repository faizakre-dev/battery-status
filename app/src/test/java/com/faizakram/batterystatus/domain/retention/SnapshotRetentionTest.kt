package com.faizakram.batterystatus.domain.retention

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotRetentionTest {

    private val hour = SnapshotRetention.BUCKET_MILLIS

    @Test
    fun `bucketStart floors to the start of the hour`() {
        assertEquals(0L, SnapshotRetention.bucketStart(0))
        assertEquals(0L, SnapshotRetention.bucketStart(hour - 1))
        assertEquals(hour, SnapshotRetention.bucketStart(hour))
        assertEquals(hour, SnapshotRetention.bucketStart(hour + 59_999))
        assertEquals(2 * hour, SnapshotRetention.bucketStart(2 * hour))
    }

    @Test
    fun `aggregateHourly groups by hour in ascending order`() {
        val snapshots = listOf(
            snapshot(ts = 2 * hour + 10, level = 50, temp = 30f),
            snapshot(ts = 0 + 10, level = 40, temp = 28f),
            snapshot(ts = 0 + 20, level = 42, temp = 30f),
        )

        val buckets = SnapshotRetention.aggregateHourly(snapshots)

        assertEquals(listOf(0L, 2 * hour), buckets.map { it.bucketStartMillis })
        assertEquals(2, buckets.first().sampleCount)
    }

    @Test
    fun `aggregateHourly averages level and temperature with min and max`() {
        val snapshots = listOf(
            snapshot(ts = 10, level = 40, temp = 28f),
            snapshot(ts = 20, level = 50, temp = 32f),
            snapshot(ts = 30, level = 60, temp = 30f),
        )

        val bucket = SnapshotRetention.aggregateHourly(snapshots).single()

        assertEquals(3, bucket.sampleCount)
        assertEquals(50f, bucket.avgLevelPercent!!, 1e-6f)
        assertEquals(40, bucket.minLevelPercent)
        assertEquals(60, bucket.maxLevelPercent)
        assertEquals(30f, bucket.avgTemperatureCelsius!!, 1e-6f)
        assertEquals(28f, bucket.minTemperatureCelsius!!, 1e-6f)
        assertEquals(32f, bucket.maxTemperatureCelsius!!, 1e-6f)
    }

    @Test
    fun `aggregateHourly tolerates missing levels and temperatures`() {
        val snapshots = listOf(
            snapshot(ts = 10, level = null, temp = null),
            snapshot(ts = 20, level = 80, temp = 25f),
        )

        val bucket = SnapshotRetention.aggregateHourly(snapshots).single()

        assertEquals(2, bucket.sampleCount)
        assertEquals(80f, bucket.avgLevelPercent!!, 1e-6f)
        assertEquals(80, bucket.minLevelPercent)
        assertEquals(80, bucket.maxLevelPercent)
        assertEquals(25f, bucket.avgTemperatureCelsius!!, 1e-6f)
    }

    @Test
    fun `aggregateHourly of only-null measurements yields null stats`() {
        val bucket = SnapshotRetention.aggregateHourly(listOf(snapshot(ts = 10, level = null, temp = null))).single()

        assertNull(bucket.avgLevelPercent)
        assertNull(bucket.minLevelPercent)
        assertNull(bucket.maxLevelPercent)
        assertNull(bucket.avgTemperatureCelsius)
    }

    @Test
    fun `aggregateHourly of empty input is empty`() {
        assertEquals(emptyList<Any>(), SnapshotRetention.aggregateHourly(emptyList()))
    }

    private fun snapshot(ts: Long, level: Int?, temp: Float?): BatterySnapshot = BatterySnapshot(
        timestampMillis = ts,
        levelPercent = level,
        scale = 100,
        status = BatteryStatus.DISCHARGING,
        health = BatteryHealth.GOOD,
        plugged = PlugType.NONE,
        voltageMv = 4_000,
        temperatureCelsius = temp,
        technology = "Li-ion",
        chargeCounterUah = null,
        currentNowUa = null,
        currentAvgUa = null,
        capacityPercent = null,
        energyCounterUah = null,
    )
}
