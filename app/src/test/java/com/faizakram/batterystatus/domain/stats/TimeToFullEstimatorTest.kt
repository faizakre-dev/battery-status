package com.faizakram.batterystatus.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeToFullEstimatorTest {

    @Test
    fun `computes a positive charge rate from increasing charge counter`() {
        val estimator = TimeToFullEstimator()

        assertNull(estimator.onSample(sample(t = 0, counter = 1_000_000, charging = true)))
        val rate = estimator.onSample(sample(t = 60_000, counter = 1_060_000, charging = true))

        assertEquals(1.0, rate!!, 1e-9)
    }

    @Test
    fun `rate is smoothed by the exponential moving average`() {
        val estimator = TimeToFullEstimator(alpha = 0.5)
        estimator.onSample(sample(t = 0, counter = 0, charging = true))
        estimator.onSample(sample(t = 60_000, counter = 60_000, charging = true))
        // Second sample adds 120_000 over 60_000 ms => instantaneous 2.0.
        val smoothed = estimator.onSample(sample(t = 120_000, counter = 180_000, charging = true))

        // 0.5 * 2.0 + 0.5 * 1.0 = 1.5
        assertEquals(1.5, smoothed!!, 1e-9)
    }

    @Test
    fun `sign flip resets the rate series`() {
        val estimator = TimeToFullEstimator()
        estimator.onSample(sample(t = 0, counter = 1_000_000, charging = true))
        estimator.onSample(sample(t = 60_000, counter = 1_060_000, charging = true))

        val afterFlip = estimator.onSample(sample(t = 120_000, counter = 1_060_000, charging = false))

        assertNull(afterFlip)
    }

    @Test
    fun `ignores samples separated by more than the max gap`() {
        val estimator = TimeToFullEstimator(maxGapMillis = 5 * 60_000L)
        estimator.onSample(sample(t = 0, counter = 1_000_000, charging = true))

        val rate = estimator.onSample(sample(t = 10 * 60_000L, counter = 2_000_000, charging = true))

        assertNull(rate)
    }

    @Test
    fun `time to full is remaining charge over the charge rate`() {
        assertEquals(
            2_940_000L,
            TimeToFullEstimator.timeToFullMillis(
                fullCapacityUah = 4_000_000,
                nowUah = 1_060_000,
                rateUaPerMs = 1.0,
            ),
        )
    }

    @Test
    fun `time to full is null when not charging or inputs are missing`() {
        assertNull(TimeToFullEstimator.timeToFullMillis(4_000_000, 1_000_000, -1.0))
        assertNull(TimeToFullEstimator.timeToFullMillis(4_000_000, 1_000_000, null))
        assertNull(TimeToFullEstimator.timeToFullMillis(null, 1_000_000, 1.0))
        assertNull(TimeToFullEstimator.timeToFullMillis(4_000_000, null, 1.0))
    }

    @Test
    fun `remaining time is charge over the discharge rate`() {
        assertEquals(
            940_000L,
            TimeToFullEstimator.remainingMillis(nowUah = 940_000, rateUaPerMs = -1.0),
        )
    }

    @Test
    fun `remaining time is null when not discharging or inputs are missing`() {
        assertNull(TimeToFullEstimator.remainingMillis(940_000, 1.0))
        assertNull(TimeToFullEstimator.remainingMillis(940_000, null))
        assertNull(TimeToFullEstimator.remainingMillis(null, -1.0))
    }

    private fun sample(t: Long, counter: Int?, charging: Boolean) = ChargeSample(
        timestampMillis = t,
        chargeCounterUah = counter,
        isCharging = charging,
    )
}
