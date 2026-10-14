package com.faizakram.batterystatus.domain.alerts

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAlertDeciderTest {

    private val decider = BatteryAlertDecider(overheatCelsius = 45f, clearCelsius = 42f)

    @Test
    fun `overheat fires once when crossing the threshold`() {
        assertEquals(listOf(BatteryAlert.OVERHEAT), decider.onSnapshot(charging(temp = 46f)))
        assertTrue(decider.onSnapshot(charging(temp = 47f)).isEmpty())
    }

    @Test
    fun `overheat does not fire below the threshold`() {
        assertTrue(decider.onSnapshot(charging(temp = 44f)).isEmpty())
    }

    @Test
    fun `overheat re-arms only after cooling below the clear threshold`() {
        assertEquals(listOf(BatteryAlert.OVERHEAT), decider.onSnapshot(charging(temp = 46f)))
        // Still above the clear threshold -> stays armed, no new alert.
        assertTrue(decider.onSnapshot(charging(temp = 43f)).isEmpty())
        // Drops to the clear threshold -> re-armed, still no alert.
        assertTrue(decider.onSnapshot(charging(temp = 42f)).isEmpty())
        // Crossing the overheat threshold again fires a fresh alert.
        assertEquals(listOf(BatteryAlert.OVERHEAT), decider.onSnapshot(charging(temp = 45f)))
    }

    @Test
    fun `overheat tolerates a missing temperature reading`() {
        assertTrue(decider.onSnapshot(charging(temp = null)).isEmpty())
    }

    @Test
    fun `full charge fires once per plugged-in episode`() {
        val full = snapshot(status = BatteryStatus.FULL, plugged = PlugType.USB)
        assertEquals(listOf(BatteryAlert.FULL_CHARGE), decider.onSnapshot(full))
        assertTrue(decider.onSnapshot(full).isEmpty())
    }

    @Test
    fun `full charge re-arms after the device is unplugged`() {
        val full = snapshot(status = BatteryStatus.FULL, plugged = PlugType.USB)
        decider.onSnapshot(full)
        decider.onSnapshot(snapshot(status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE))
        assertEquals(listOf(BatteryAlert.FULL_CHARGE), decider.onSnapshot(full))
    }

    @Test
    fun `charging below full does not trigger the full alert`() {
        assertTrue(decider.onSnapshot(charging(temp = 35f)).isEmpty())
    }

    @Test
    fun `reset re-arms all alerts`() {
        decider.onSnapshot(charging(temp = 46f))
        val full = snapshot(status = BatteryStatus.FULL, plugged = PlugType.USB)
        decider.onSnapshot(full)

        decider.reset()

        assertEquals(listOf(BatteryAlert.OVERHEAT), decider.onSnapshot(charging(temp = 46f)))
        assertEquals(listOf(BatteryAlert.FULL_CHARGE), decider.onSnapshot(full))
    }

    private fun charging(temp: Float?): BatterySnapshot =
        snapshot(status = BatteryStatus.CHARGING, plugged = PlugType.USB, temp = temp)

    private fun snapshot(
        status: BatteryStatus,
        plugged: PlugType,
        temp: Float? = 30f,
    ): BatterySnapshot = BatterySnapshot(
        timestampMillis = 1_000,
        levelPercent = 80,
        scale = 100,
        status = status,
        health = BatteryHealth.GOOD,
        plugged = plugged,
        voltageMv = 4_100,
        temperatureCelsius = temp,
        technology = "Li-ion",
        chargeCounterUah = null,
        currentNowUa = null,
        currentAvgUa = null,
        capacityPercent = null,
        energyCounterUah = null,
    )
}
