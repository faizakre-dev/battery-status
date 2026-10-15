package com.faizakram.batterystatus.session

import com.faizakram.batterystatus.testutil.FakeBatteryRepository
import com.faizakram.batterystatus.testutil.chargingSnapshot
import com.faizakram.batterystatus.testutil.unpluggedSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeSessionRecorderTest {

    private val repository = FakeBatteryRepository()
    private val recorder = ChargeSessionRecorder(repository)

    @Test
    fun `charging snapshot opens a persisted session`() = runTest {
        val closed = recorder.onSnapshot(chargingSnapshot(ts = 1_000, percent = 40))

        assertNull(closed)
        assertNotNull(repository.open)
        assertEquals(40, repository.open?.percentFrom)
        assertTrue(repository.sessions.isEmpty())
    }

    @Test
    fun `session survives a recorder restart`() = runTest {
        recorder.onSnapshot(chargingSnapshot(ts = 1_000, percent = 40))

        // Simulate process death: a brand-new recorder over the same storage.
        val restarted = ChargeSessionRecorder(repository)
        val closed = restarted.onSnapshot(unpluggedSnapshot(ts = 2_000, percent = 80))

        assertNotNull(closed)
        assertEquals(1, repository.sessions.size)
        assertEquals(40, closed?.percentFrom)
        assertEquals(80, closed?.percentTo)
    }

    @Test
    fun `unplug closes and persists the session exactly once`() = runTest {
        recorder.onSnapshot(chargingSnapshot(ts = 1_000, percent = 40))
        recorder.onSnapshot(chargingSnapshot(ts = 1_500, percent = 60))
        val closed = recorder.onSnapshot(unpluggedSnapshot(ts = 2_000, percent = 80))

        assertNotNull(closed)
        assertEquals(1_000L, closed?.startMillis)
        assertEquals(1_500L, closed?.endMillis)
        assertEquals(1, repository.sessions.size)
        assertNull(repository.open)

        // A second unplug snapshot must not create a duplicate session.
        val again = recorder.onSnapshot(unpluggedSnapshot(ts = 3_000, percent = 70))
        assertNull(again)
        assertEquals(1, repository.sessions.size)
    }

    @Test
    fun `unplug without a charging session records nothing`() = runTest {
        val closed = recorder.onSnapshot(unpluggedSnapshot(ts = 1_000, percent = 40))

        assertNull(closed)
        assertTrue(repository.sessions.isEmpty())
    }

    @Test
    fun `explicit charging flag overrides a stale snapshot`() = runTest {
        // The sticky broadcast still says unplugged, but POWER_CONNECTED fired.
        recorder.onSnapshot(unpluggedSnapshot(ts = 1_000, percent = 40), isCharging = true)

        assertNotNull(repository.open)

        // And the reverse: a stale "plugged" snapshot on disconnect closes it.
        recorder.onSnapshot(chargingSnapshot(ts = 2_000, percent = 50), isCharging = false)
        assertEquals(1, repository.sessions.size)
        assertNull(repository.open)
    }
}
