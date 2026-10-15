package com.faizakram.batterystatus.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenChargeSessionTest {

    @Test
    fun `start captures the first snapshot`() {
        val open = OpenChargeSession.start(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.USB),
        )

        assertEquals(1_000L, open.startMillis)
        assertEquals(40, open.percentFrom)
        assertFalse(open.reachedFull)
    }

    @Test
    fun `accumulate keeps the maximum percent and temperature`() {
        val open = OpenChargeSession.start(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, tempCelsius = 30f),
        )
            .accumulate(snapshot(ts = 2_000, percent = 60, status = BatteryStatus.CHARGING, plugged = PlugType.AC, tempCelsius = 35f))
            .accumulate(snapshot(ts = 3_000, percent = 55, status = BatteryStatus.CHARGING, plugged = PlugType.AC, tempCelsius = 33f))

        assertEquals(60, open.maxPercent)
        assertEquals(35f, open.maxTemperatureCelsius!!, 0.001f)
    }

    @Test
    fun `close computes stats from charge counter and temperatures`() {
        val open = OpenChargeSession.start(
            snapshot(
                ts = 1_000,
                percent = 40,
                status = BatteryStatus.CHARGING,
                plugged = PlugType.USB,
                tempCelsius = 30f,
                currentUa = -500_000,
                chargeCounterUah = 1_000_000,
            ),
        )
            .accumulate(
                snapshot(
                    ts = 2_000,
                    percent = 70,
                    status = BatteryStatus.CHARGING,
                    plugged = PlugType.USB,
                    tempCelsius = 35f,
                    currentUa = -1_500_000,
                    chargeCounterUah = 1_500_000,
                ),
            )
            .accumulate(
                snapshot(
                    ts = 3_000,
                    percent = 95,
                    status = BatteryStatus.CHARGING,
                    plugged = PlugType.USB,
                    tempCelsius = 33f,
                    currentUa = -1_000_000,
                    chargeCounterUah = 2_000_000,
                ),
            )

        val session = open.close(
            snapshot(
                ts = 4_000,
                percent = 100,
                status = BatteryStatus.DISCHARGING,
                plugged = PlugType.NONE,
                tempCelsius = 31f,
                chargeCounterUah = 2_400_000,
            ),
        )

        assertEquals(1_000L, session.startMillis)
        assertEquals(3_000L, session.endMillis)
        assertEquals(40, session.percentFrom)
        assertEquals(100, session.percentTo)
        assertEquals(1_400, session.mahAdded)
        assertEquals(35f, session.maxTemperatureCelsius!!, 0.001f)
        assertEquals(-1_000_000, session.avgCurrentUa)
        assertTrue(session.isFull)
    }

    @Test
    fun `close ends at the last charging sample so idle time is excluded`() {
        val session = OpenChargeSession.start(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 1_000_000),
        )
            .accumulate(snapshot(ts = 2_000, percent = 60, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 1_400_000))
            .close(
                // Closed much later because the unplug broadcast was never seen.
                snapshot(ts = 9_999_999, percent = 58, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE, chargeCounterUah = 1_400_000),
            )

        assertEquals(1_000L, session.startMillis)
        assertEquals(2_000L, session.endMillis)
    }

    @Test
    fun `clamps negative charge counter deltas to zero`() {
        val session = OpenChargeSession.start(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 2_000_000),
        ).close(
            snapshot(ts = 2_000, percent = 55, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE, chargeCounterUah = 1_000_000),
        )

        assertEquals(0, session.mahAdded)
    }

    @Test
    fun `leaves avg current null when no current samples exist`() {
        val session = OpenChargeSession.start(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 1_000_000),
        ).close(
            snapshot(ts = 2_000, percent = 90, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE, chargeCounterUah = 1_400_000),
        )

        assertNull(session.avgCurrentUa)
        assertEquals(400, session.mahAdded)
    }

    private fun snapshot(
        ts: Long,
        percent: Int?,
        status: BatteryStatus,
        plugged: PlugType,
        tempCelsius: Float? = null,
        currentUa: Int? = null,
        chargeCounterUah: Int? = null,
    ): BatterySnapshot = BatterySnapshot(
        timestampMillis = ts,
        levelPercent = percent,
        scale = 100,
        status = status,
        health = BatteryHealth.GOOD,
        plugged = plugged,
        voltageMv = 4_300,
        temperatureCelsius = tempCelsius,
        technology = "Li-ion",
        chargeCounterUah = chargeCounterUah,
        currentNowUa = currentUa,
        currentAvgUa = null,
        capacityPercent = null,
        energyCounterUah = null,
    )
}
