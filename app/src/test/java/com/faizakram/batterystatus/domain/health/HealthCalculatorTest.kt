package com.faizakram.batterystatus.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthCalculatorTest {

    @Test
    fun `computes health percentage from full and design capacity`() {
        assertEquals(90, HealthCalculator.healthPercent(3_600_000, 4_000_000))
        assertEquals(100, HealthCalculator.healthPercent(4_000_000, 4_000_000))
    }

    @Test
    fun `returns null when either capacity is missing or invalid`() {
        assertNull(HealthCalculator.healthPercent(null, 4_000_000))
        assertNull(HealthCalculator.healthPercent(3_600_000, null))
        assertNull(HealthCalculator.healthPercent(3_600_000, 0))
        assertNull(HealthCalculator.healthPercent(-1, 4_000_000))
    }

    @Test
    fun `clamps implausible values into the 0 to 100 range`() {
        assertEquals(100, HealthCalculator.healthPercent(5_000_000, 4_000_000))
    }
}
