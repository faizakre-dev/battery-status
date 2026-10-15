package com.faizakram.batterystatus.receiver

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.faizakram.batterystatus.session.ChargeSessionRecorder
import com.faizakram.batterystatus.testutil.FakeBatteryInfoSource
import com.faizakram.batterystatus.testutil.FakeBatteryRepository
import com.faizakram.batterystatus.testutil.chargingSnapshot
import com.faizakram.batterystatus.testutil.unpluggedSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PowerConnectionHandlerTest {

    private val context = RecordingContext(
        ApplicationProvider.getApplicationContext<Application>(),
    )
    private val info = FakeBatteryInfoSource(chargingSnapshot(ts = 1_000, percent = 40))
    private val repository = FakeBatteryRepository()
    private val handler = PowerConnectionHandler(
        batteryInfoSource = info,
        repository = repository,
        recorder = ChargeSessionRecorder(repository),
    )

    @Test
    fun `plug records a snapshot, opens the session and starts the service`() = runTest {
        handler.handle(context, isCharging = true)

        assertEquals(1, repository.snapshots.size)
        assertNotNull(repository.open)
        assertTrue(repository.sessions.isEmpty())
        assertEquals(1, context.startedServices.size)
        assertTrue(
            context.startedServices.single().component
                ?.className.orEmpty()
                .endsWith("ChargeTrackingService"),
        )
    }

    @Test
    fun `unplug closes the persisted session and does not start the service`() = runTest {
        handler.handle(context, isCharging = true)

        info.snapshot = unpluggedSnapshot(ts = 2_000, percent = 80)
        handler.handle(context, isCharging = false)

        assertEquals(1, repository.sessions.size)
        assertNull(repository.open)
        // Only the earlier plug started the service.
        assertEquals(1, context.startedServices.size)
    }

    @Test
    fun `explicit charging flag wins over a stale snapshot`() = runTest {
        // Snapshot still says unplugged, but the POWER_CONNECTED broadcast fired.
        info.snapshot = unpluggedSnapshot(ts = 1_000, percent = 40)

        handler.handle(context, isCharging = true)

        assertNotNull(repository.open)
        assertEquals(1, context.startedServices.size)
    }

    @Test
    fun `service start failure still records the session`() = runTest {
        context.failStart = true

        handler.handle(context, isCharging = true)

        assertNotNull(repository.open)
        assertTrue(context.startedServices.isEmpty())
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        val startedServices = mutableListOf<Intent>()
        var failStart = false

        override fun startForegroundService(service: Intent): ComponentName? {
            if (failStart) throw SecurityException("background start not allowed")
            startedServices += service
            return service.component
        }
    }
}
