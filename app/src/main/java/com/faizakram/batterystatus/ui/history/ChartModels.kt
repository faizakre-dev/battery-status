package com.faizakram.batterystatus.ui.history

import com.faizakram.batterystatus.core.model.BatterySnapshot

/** A single point on a time series: [x] is epoch millis, [y] the value. */
data class ChartPoint(val x: Long, val y: Float)

/** A renderable series plus its vertical bounds (fixed for level, auto for temperature). */
data class ChartSeries(
    val points: List<ChartPoint>,
    val minY: Float,
    val maxY: Float,
) {
    val isEmpty: Boolean get() = points.isEmpty()

    /** Convenience accessors used for the min/max caption under each chart. */
    val minValue: Float? get() = points.minOfOrNull { it.y }
    val maxValue: Float? get() = points.maxOfOrNull { it.y }

    companion object {
        val EMPTY = ChartSeries(emptyList(), 0f, 0f)
    }
}

/**
 * Pure transforms that turn persisted snapshots into chart-ready series. Kept
 * free of Android/Compose types so it is unit-testable on the JVM.
 */
object ChartModels {

    /** Cap the number of rendered points so a 7-day range stays smooth. */
    const val MAX_POINTS = 180

    fun levelSeries(snapshots: List<BatterySnapshot>): ChartSeries =
        series(
            points = snapshots.mapNotNull { snapshot ->
                snapshot.levelPercent?.let { ChartPoint(snapshot.timestampMillis, it.toFloat()) }
            },
            fixedMin = 0f,
            fixedMax = 100f,
        )

    fun temperatureSeries(snapshots: List<BatterySnapshot>): ChartSeries =
        series(
            points = snapshots.mapNotNull { snapshot ->
                snapshot.temperatureCelsius?.let { ChartPoint(snapshot.timestampMillis, it) }
            },
        )

    fun series(
        points: List<ChartPoint>,
        fixedMin: Float? = null,
        fixedMax: Float? = null,
    ): ChartSeries {
        val sampled = downsample(points.sortedBy { it.x }, MAX_POINTS)
        if (sampled.isEmpty()) return ChartSeries.EMPTY

        var minY = fixedMin ?: sampled.minOf { it.y }
        var maxY = fixedMax ?: sampled.maxOf { it.y }
        if (maxY <= minY) maxY = minY + 1f

        return ChartSeries(sampled, minY, maxY)
    }

    /**
     * Reduces [points] to at most [maxPoints] by averaging equal-sized buckets.
     * The first and last x values are preserved so the time axis stays accurate.
     */
    fun downsample(points: List<ChartPoint>, maxPoints: Int): List<ChartPoint> {
        if (maxPoints <= 0) return emptyList()
        if (points.size <= maxPoints) return points

        val bucketSize = points.size.toDouble() / maxPoints
        return (0 until maxPoints).map { index ->
            val from = (index * bucketSize).toInt()
            val to = (((index + 1) * bucketSize).toInt()).coerceIn(from + 1, points.size)
            val slice = points.subList(from, to)
            ChartPoint(x = slice.last().x, y = slice.map { it.y }.average().toFloat())
        }
    }
}
