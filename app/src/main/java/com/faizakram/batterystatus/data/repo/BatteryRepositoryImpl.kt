package com.faizakram.batterystatus.data.repo

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.HourlySnapshot
import com.faizakram.batterystatus.data.local.dao.BatteryEstimatesDao
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
import com.faizakram.batterystatus.data.local.dao.HourlySnapshotDao
import com.faizakram.batterystatus.data.local.entity.BatteryEstimatesEntity
import com.faizakram.batterystatus.data.local.toDomain
import com.faizakram.batterystatus.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    private val snapshotDao: BatterySnapshotDao,
    private val sessionDao: ChargeSessionDao,
    private val estimatesDao: BatteryEstimatesDao,
    private val hourlySnapshotDao: HourlySnapshotDao,
) : BatteryRepository {

    override suspend fun recordSnapshot(snapshot: BatterySnapshot): Long =
        snapshotDao.insert(snapshot.toEntity())

    override suspend fun recordSession(session: ChargeSession): Long =
        sessionDao.insert(session.toEntity())

    override fun observeLatestSnapshot(): Flow<BatterySnapshot?> =
        snapshotDao.observeLatest().map { it?.toDomain() }

    override fun observeSnapshotsSince(sinceMillis: Long): Flow<List<BatterySnapshot>> =
        snapshotDao.observeSince(sinceMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun snapshotsBefore(cutoffMillis: Long): List<BatterySnapshot> =
        snapshotDao.getBefore(cutoffMillis).map { it.toDomain() }

    override fun observeSessions(): Flow<List<ChargeSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun snapshotCount(): Int = snapshotDao.count()

    override suspend fun trimSnapshotsBefore(cutoffMillis: Long): Int =
        snapshotDao.deleteBefore(cutoffMillis)

    override fun observeHourlySnapshotsSince(sinceMillis: Long): Flow<List<HourlySnapshot>> =
        hourlySnapshotDao.observeSince(sinceMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun saveHourlySnapshots(buckets: List<HourlySnapshot>): Int {
        if (buckets.isEmpty()) return 0
        return hourlySnapshotDao.upsertAll(buckets.map { it.toEntity() }).count { it != -1L }
    }

    override suspend fun hourlySnapshotCount(): Int = hourlySnapshotDao.count()

    override fun observeEstimates(): Flow<BatteryEstimates?> =
        estimatesDao.observe(BatteryEstimatesEntity.SINGLETON_ID).map { it?.toDomain() }

    override suspend fun getEstimates(): BatteryEstimates? =
        estimatesDao.get(BatteryEstimatesEntity.SINGLETON_ID)?.toDomain()

    override suspend fun saveEstimates(estimates: BatteryEstimates): Long =
        estimatesDao.upsert(estimates.toEntity())
}
