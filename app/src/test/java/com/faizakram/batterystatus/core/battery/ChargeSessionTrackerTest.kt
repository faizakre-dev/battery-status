package com.faizakram.batterystatus.core.battery

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeSessionTrackerTest {

    private val tracker = ChargeSessionTracker()

    @Test
    fun `opens a session on the first charging snapshot`() {
        val events = tracker.onSnapshot(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.USB),
        )

        assertEquals(1, events.size)
        assertTrue(events[0] is TrackerEvent.SessionStarted)
        assertTrue(tracker.hasOpenSession)
    }

    @Test
    fun `does nothing while not charging`() {
        val events = tracker.onSnapshot(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE),
        )

        assertTrue(events.isEmpty())
        assertFalse(tracker.hasOpenSession)
    }

    @Test
    fun `stays open across consecutive charging snapshots without re-emitting`() {
        tracker.onSnapshot(snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC))
        val second = tracker.onSnapshot(snapshot(ts = 2_000, percent = 60, status = BatteryStatus.CHARGING, plugged = PlugType.AC))
        val third = tracker.onSnapshot(snapshot(ts = 3_000, percent = 80, status = BatteryStatus.FULL, plugged = PlugType.AC))

        assertTrue(second.isEmpty())
        assertTrue(third.isEmpty())
        assertTrue(tracker.hasOpenSession)
    }

    @Test
    fun `closes session and computes stats from charge counter and temperatures`() {
        tracker.onSnapshot(
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
        tracker.onSnapshot(
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
        tracker.onSnapshot(
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

        val events = tracker.onSnapshot(
            snapshot(
                ts = 4_000,
                percent = 100,
                status = BatteryStatus.DISCHARGING,
                plugged = PlugType.NONE,
                tempCelsius = 31f,
                chargeCounterUah = 2_400_000,
            ),
        )

        assertEquals(1, events.size)
        val closed = events[0] as TrackerEvent.SessionClosed
        val session: ChargeSession = closed.session

        assertEquals(1_000L, session.startMillis)
        assertEquals(4_000L, session.endMillis)
        assertEquals(40, session.percentFrom)
        assertEquals(100, session.percentTo)
        assertEquals(1_400, session.mahAdded)
        assertEquals(35f, session.maxTemperatureCelsius!!, 0.001f)
        assertEquals(-1_000_000, session.avgCurrentUa)
        assertTrue(session.isFull)
        assertFalse(tracker.hasOpenSession)
    }

    @Test
    fun `clamps negative charge counter deltas to zero`() {
        tracker.onSnapshot(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 2_000_000),
        )
        val events = tracker.onSnapshot(
            snapshot(ts = 2_000, percent = 55, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE, chargeCounterUah = 1_000_000),
        )

        val session = (events[0] as TrackerEvent.SessionClosed).session
        assertEquals(0, session.mahAdded)
    }

    @Test
    fun `leaves avg current null when no current samples exist`() {
        tracker.onSnapshot(
            snapshot(ts = 1_000, percent = 40, status = BatteryStatus.CHARGING, plugged = PlugType.AC, chargeCounterUah = 1_000_000),
        )
        val events = tracker.onSnapshot(
            snapshot(ts = 2_000, percent = 90, status = BatteryStatus.DISCHARGING, plugged = PlugType.NONE, chargeCounterUah = 1_400_000),
        )

        val session = (events[0] as TrackerEvent.SessionClosed).session
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
