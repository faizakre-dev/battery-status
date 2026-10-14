package com.faizakram.batterystatus.core.health

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryHealthEvaluatorTest {

    @Test
    fun `good health at room temperature is GOOD`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.GOOD, temperature = 28f),
        )

        assertEquals(HealthLevel.GOOD, result.level)
        assertEquals(listOf(HealthReason.HEALTHY), result.reasons)
    }

    @Test
    fun `overheat health is POOR`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.OVERHEAT, temperature = 41f),
        )

        assertEquals(HealthLevel.POOR, result.level)
        assertEquals(listOf(HealthReason.OVERHEAT), result.reasons)
    }

    @Test
    fun `cold health is ATTENTION`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.COLD, temperature = 5f),
        )

        assertEquals(HealthLevel.ATTENTION, result.level)
        assertEquals(listOf(HealthReason.COLD), result.reasons)
    }

    @Test
    fun `dead and over voltage and failure are POOR`() {
        assertEquals(
            HealthLevel.POOR,
            BatteryHealthEvaluator.assess(snapshot(health = BatteryHealth.DEAD)).level,
        )
        assertEquals(
            HealthLevel.POOR,
            BatteryHealthEvaluator.assess(snapshot(health = BatteryHealth.OVER_VOLTAGE)).level,
        )
        assertEquals(
            HealthLevel.POOR,
            BatteryHealthEvaluator.assess(snapshot(health = BatteryHealth.UNSPECIFIED_FAILURE)).level,
        )
    }

    @Test
    fun `unknown health is UNKNOWN`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.UNKNOWN, temperature = 28f),
        )

        assertEquals(HealthLevel.UNKNOWN, result.level)
        assertEquals(listOf(HealthReason.UNKNOWN), result.reasons)
    }

    @Test
    fun `warning temperature downgrades a good reading to ATTENTION`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.GOOD, temperature = 42f),
        )

        assertEquals(HealthLevel.ATTENTION, result.level)
        assertEquals(
            listOf(HealthReason.HEALTHY, HealthReason.TEMPERATURE_HIGH),
            result.reasons,
        )
    }

    @Test
    fun `critical temperature downgrades to POOR`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.GOOD, temperature = 46f),
        )

        assertEquals(HealthLevel.POOR, result.level)
    }

    @Test
    fun `freezing temperature downgrades to ATTENTION`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.GOOD, temperature = -2f),
        )

        assertEquals(HealthLevel.ATTENTION, result.level)
        assertEquals(
            listOf(HealthReason.HEALTHY, HealthReason.TEMPERATURE_LOW),
            result.reasons,
        )
    }

    @Test
    fun `missing temperature only reflects the reported health`() {
        val result = BatteryHealthEvaluator.assess(
            snapshot(health = BatteryHealth.GOOD, temperature = null),
        )

        assertEquals(HealthLevel.GOOD, result.level)
        assertEquals(listOf(HealthReason.HEALTHY), result.reasons)
    }

    private fun snapshot(
        health: BatteryHealth,
        temperature: Float? = 28f,
    ): BatterySnapshot = BatterySnapshot(
        timestampMillis = 0L,
        levelPercent = 50,
        scale = 100,
        status = BatteryStatus.DISCHARGING,
        health = health,
        plugged = PlugType.NONE,
        voltageMv = 4000,
        temperatureCelsius = temperature,
        technology = "Li-ion",
        chargeCounterUah = null,
        currentNowUa = null,
        currentAvgUa = null,
        capacityPercent = 50,
        energyCounterUah = null,
    )
}
