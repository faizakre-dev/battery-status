package com.faizakram.batterystatus.ui.history

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {

    @Test
    fun `sessions are ordered newest first`() {
        val state = HistoryUiState.from(
            range = HistoryRange.WEEK,
            snapshots = emptyList(),
            sessions = listOf(
                session(id = 1, start = 1_000),
                session(id = 2, start = 9_000),
                session(id = 3, start = 5_000),
            ),
        )

        assertEquals(listOf(2L, 3L, 1L), state.sessions.map { it.id })
    }

    @Test
    fun `session row formats duration, temperature and mAh`() {
        val ui = session(
            id = 1,
            start = 0,
            end = 90 * 60 * 1000,
            mahAdded = 1_400,
            maxTemp = 35.3f,
        ).toSessionUi()

        assertEquals("1h 30m", ui.durationText)
        assertEquals("35.3 °C", ui.maxTemperatureText)
        assertEquals(1_400, ui.mahAdded)
    }

    @Test
    fun `session without measurable duration has null duration text`() {
        val ui = session(id = 1, start = 1_000, end = 1_000).toSessionUi()

        assertEquals(null, ui.durationText)
    }

    @Test
    fun `state reports data presence`() {
        val populated = HistoryUiState.from(
            range = HistoryRange.DAY,
            snapshots = listOf(snapshot(ts = 1, level = 50, temp = 30f)),
            sessions = emptyList(),
        )
        val empty = HistoryUiState.from(HistoryRange.DAY, emptyList(), emptyList())

        assertTrue(populated.hasAnyData)
        assertFalse(empty.hasAnyData)
    }

    @Test
    fun `range windows map to look-back cutoffs`() {
        val now = 1_000_000_000_000L

        assertEquals(now - 24L * 60 * 60 * 1000, HistoryRange.DAY.sinceMillis(now))
        assertEquals(now - 7L * 24 * 60 * 60 * 1000, HistoryRange.WEEK.sinceMillis(now))
        assertEquals(0L, HistoryRange.ALL.sinceMillis(now))
    }

    private fun session(
        id: Long,
        start: Long,
        end: Long = start + 60_000,
        mahAdded: Int? = null,
        maxTemp: Float? = null,
    ): ChargeSession = ChargeSession(
        id = id,
        startMillis = start,
        endMillis = end,
        percentFrom = 40,
        percentTo = 100,
        mahAdded = mahAdded,
        maxTemperatureCelsius = maxTemp,
        avgCurrentUa = null,
        isFull = true,
    )

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
