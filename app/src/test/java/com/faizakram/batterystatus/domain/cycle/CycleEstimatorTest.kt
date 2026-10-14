package com.faizakram.batterystatus.domain.cycle

import org.junit.Assert.assertEquals
import org.junit.Test

class CycleEstimatorTest {

    @Test
    fun `session contribution is the fraction of one design cycle`() {
        // 1000 mAh added out of a 4000 mAh design capacity.
        assertEquals(0.25, CycleEstimator.sessionContribution(1_000, 4_000_000), 1e-9)
    }

    @Test
    fun `session contribution is zero when inputs are missing or invalid`() {
        assertEquals(0.0, CycleEstimator.sessionContribution(null, 4_000_000), 1e-9)
        assertEquals(0.0, CycleEstimator.sessionContribution(1_000, null), 1e-9)
        assertEquals(0.0, CycleEstimator.sessionContribution(0, 4_000_000), 1e-9)
    }

    @Test
    fun `session contribution is clamped to guard against noisy counters`() {
        assertEquals(1.5, CycleEstimator.sessionContribution(9_000, 4_000_000), 1e-9)
    }

    @Test
    fun `effective cycles prefers the kernel value when available`() {
        assertEquals(142.0, CycleEstimator.effectiveCycles(3.5, 142), 1e-9)
    }

    @Test
    fun `effective cycles falls back to the accumulated fraction`() {
        assertEquals(3.5, CycleEstimator.effectiveCycles(3.5, null), 1e-9)
        assertEquals(0.0, CycleEstimator.effectiveCycles(-1.0, null), 1e-9)
    }
}
