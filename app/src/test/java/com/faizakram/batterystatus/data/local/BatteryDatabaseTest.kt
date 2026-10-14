package com.faizakram.batterystatus.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.PlugType
import com.faizakram.batterystatus.data.local.dao.BatteryEstimatesDao
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
import com.faizakram.batterystatus.data.local.entity.BatteryEstimatesEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BatteryDatabaseTest {

    private lateinit var database: BatteryDatabase
    private lateinit var snapshotDao: BatterySnapshotDao
    private lateinit var sessionDao: ChargeSessionDao
    private lateinit var estimatesDao: BatteryEstimatesDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, BatteryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        snapshotDao = database.batterySnapshotDao()
        sessionDao = database.chargeSessionDao()
        estimatesDao = database.batteryEstimatesDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inserting a snapshot increases the count`() = runBlocking {
        snapshotDao.insert(snapshot(ts = 1_000, percent = 40).toEntity())
        snapshotDao.insert(snapshot(ts = 2_000, percent = 75).toEntity())

        assertEquals(2, snapshotDao.count())
    }

    @Test
    fun `observeLatest returns the newest snapshot`() = runBlocking {
        snapshotDao.insert(snapshot(ts = 1_000, percent = 40).toEntity())
        snapshotDao.insert(snapshot(ts = 9_000, percent = 88).toEntity())

        val latest = snapshotDao.observeLatest().first()

        assertNotNull(latest)
        assertEquals(88, latest?.levelPercent)
        assertEquals(9_000L, latest?.timestampMillis)
    }

    @Test
    fun `observeSince filters out older snapshots`() = runBlocking {
        snapshotDao.insert(snapshot(ts = 1_000, percent = 10).toEntity())
        snapshotDao.insert(snapshot(ts = 5_000, percent = 20).toEntity())
        snapshotDao.insert(snapshot(ts = 9_000, percent = 30).toEntity())

        val since = snapshotDao.observeSince(5_000).first()

        assertEquals(listOf(5_000L, 9_000L), since.map { it.timestampMillis })
    }

    @Test
    fun `deleteBefore removes only older rows`() = runBlocking {
        snapshotDao.insert(snapshot(ts = 1_000, percent = 10).toEntity())
        snapshotDao.insert(snapshot(ts = 5_000, percent = 20).toEntity())
        snapshotDao.insert(snapshot(ts = 9_000, percent = 30).toEntity())

        val removed = snapshotDao.deleteBefore(5_000)

        assertEquals(1, removed)
        assertEquals(2, snapshotDao.count())
    }

    @Test
    fun `round-trips a charge session including temperature`() = runBlocking {
        sessionDao.insert(
            ChargeSession(
                startMillis = 1_000,
                endMillis = 4_000,
                percentFrom = 40,
                percentTo = 100,
                mahAdded = 1_400,
                maxTemperatureCelsius = 35.3f,
                avgCurrentUa = -1_000_000,
                isFull = true,
            ).toEntity(),
        )

        val sessions = sessionDao.observeAll().first()

        assertEquals(1, sessions.size)
        val stored = sessions.first()
        assertEquals(40, stored.percentFrom)
        assertEquals(100, stored.percentTo)
        assertEquals(1_400, stored.mahAdded)
        assertEquals(353, stored.maxTemperatureDeciCelsius)
        assertEquals(-1_000_000, stored.avgCurrentUa)
        assertEquals(true, stored.isFull)
        assertEquals(1, sessionDao.fullChargeSessions().size)
    }

    @Test
    fun `estimates round-trip through the singleton row`() = runBlocking {
        estimatesDao.upsert(
            BatteryEstimates(
                designCapacityUah = 4_000_000,
                lifetimePeakUah = 3_900_000,
                accumulatedCycleFraction = 12.5,
                sysfsCycleCount = null,
                lastUpdatedMillis = 7_000,
            ).toEntity(),
        )

        val stored = estimatesDao.get(BatteryEstimatesEntity.SINGLETON_ID)?.toDomain()

        assertNotNull(stored)
        assertEquals(4_000_000, stored?.designCapacityUah)
        assertEquals(3_900_000, stored?.lifetimePeakUah)
        assertEquals(12.5, stored!!.accumulatedCycleFraction, 1e-9)
        assertEquals(7_000L, stored.lastUpdatedMillis)
    }

    @Test
    fun `estimates upsert replaces the previous row`() = runBlocking {
        estimatesDao.upsert(
            BatteryEstimates.EMPTY.copy(accumulatedCycleFraction = 1.0, lastUpdatedMillis = 1).toEntity(),
        )
        estimatesDao.upsert(
            BatteryEstimates.EMPTY.copy(accumulatedCycleFraction = 2.0, lastUpdatedMillis = 2).toEntity(),
        )

        val observed = estimatesDao.observe(BatteryEstimatesEntity.SINGLETON_ID).first()

        assertEquals(2.0, observed!!.accumulatedCycleFraction, 1e-9)
        assertEquals(2L, observed.lastUpdatedMillis)
    }

    @Test
    fun `estimates are absent before first write`() = runBlocking {
        assertNull(estimatesDao.get(BatteryEstimatesEntity.SINGLETON_ID))
    }

    private fun snapshot(ts: Long, percent: Int): BatterySnapshot = BatterySnapshot(
        timestampMillis = ts,
        levelPercent = percent,
        scale = 100,
        status = BatteryStatus.CHARGING,
        health = BatteryHealth.GOOD,
        plugged = PlugType.USB,
        voltageMv = 4_300,
        temperatureCelsius = 30f,
        technology = "Li-ion",
        chargeCounterUah = null,
        currentNowUa = null,
        currentAvgUa = null,
        capacityPercent = null,
        energyCounterUah = null,
    )
}
