package com.faizakram.batterystatus.ui.history

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartModelsTest {

    @Test
    fun `levelSeries uses a fixed 0 to 100 vertical range`() {
        val series = ChartModels.levelSeries(
            listOf(snapshot(ts = 1, level = 55, temp = 30f), snapshot(ts = 2, level = 60, temp = 31f)),
        )

        assertEquals(0f, series.minY, 1e-6f)
        assertEquals(100f, series.maxY, 1e-6f)
        assertEquals(2, series.points.size)
        assertEquals(55f, series.points.first().y, 1e-6f)
    }

    @Test
    fun `levelSeries skips snapshots without a level`() {
        val series = ChartModels.levelSeries(
            listOf(snapshot(ts = 1, level = null, temp = 30f), snapshot(ts = 2, level = 60, temp = 31f)),
        )

        assertEquals(1, series.points.size)
        assertEquals(2L, series.points.first().x)
    }

    @Test
    fun `temperatureSeries maps celsius values and auto-scales`() {
        val series = ChartModels.temperatureSeries(
            listOf(snapshot(ts = 1, level = 50, temp = 28f), snapshot(ts = 2, level = 50, temp = 35f)),
        )

        assertEquals(28f, series.minY, 1e-6f)
        assertEquals(35f, series.maxY, 1e-6f)
        assertEquals(2, series.points.size)
    }

    @Test
    fun `empty input yields the empty series`() {
        assertTrue(ChartModels.levelSeries(emptyList()).isEmpty)
        assertTrue(ChartModels.temperatureSeries(emptyList()).isEmpty)
    }

    @Test
    fun `series sorts points by timestamp`() {
        val series = ChartModels.series(
            listOf(ChartPoint(x = 3, y = 1f), ChartPoint(x = 1, y = 2f), ChartPoint(x = 2, y = 3f)),
        )

        assertEquals(listOf(1L, 2L, 3L), series.points.map { it.x })
    }

    @Test
    fun `downsample reduces to the cap and keeps the newest timestamp`() {
        val points = (1..1000).map { ChartPoint(x = it.toLong(), y = it.toFloat()) }

        val sampled = ChartModels.downsample(points, maxPoints = 100)

        assertEquals(100, sampled.size)
        assertEquals(10L, sampled.first().x)
        assertEquals(1000L, sampled.last().x)
    }

    @Test
    fun `downsample leaves lists at or below the cap untouched`() {
        val points = (1..10).map { ChartPoint(x = it.toLong(), y = it.toFloat()) }

        assertEquals(points, ChartModels.downsample(points, maxPoints = 180))
    }

    @Test
    fun `downsample averages values within a bucket`() {
        val points = listOf(
            ChartPoint(1, 10f),
            ChartPoint(2, 20f),
            ChartPoint(3, 30f),
            ChartPoint(4, 40f),
        )

        val sampled = ChartModels.downsample(points, maxPoints = 2)

        assertEquals(2, sampled.size)
        assertEquals(15f, sampled[0].y, 1e-6f)
        assertEquals(35f, sampled[1].y, 1e-6f)
    }

    @Test
    fun `single point keeps a positive vertical span`() {
        val series = ChartModels.series(listOf(ChartPoint(x = 5, y = 42f)))

        assertFalse(series.isEmpty)
        assertTrue(series.maxY > series.minY)
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
