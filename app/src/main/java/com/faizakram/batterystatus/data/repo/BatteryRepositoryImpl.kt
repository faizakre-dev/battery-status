package com.faizakram.batterystatus.data.repo

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
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
) : BatteryRepository {

    override suspend fun recordSnapshot(snapshot: BatterySnapshot): Long =
        snapshotDao.insert(snapshot.toEntity())

    override suspend fun recordSession(session: ChargeSession): Long =
        sessionDao.insert(session.toEntity())

    override fun observeLatestSnapshot(): Flow<BatterySnapshot?> =
        snapshotDao.observeLatest().map { it?.toDomain() }

    override fun observeSnapshotsSince(sinceMillis: Long): Flow<List<BatterySnapshot>> =
        snapshotDao.observeSince(sinceMillis).map { list -> list.map { it.toDomain() } }

    override fun observeSessions(): Flow<List<ChargeSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun snapshotCount(): Int = snapshotDao.count()

    override suspend fun trimSnapshotsBefore(cutoffMillis: Long): Int =
        snapshotDao.deleteBefore(cutoffMillis)
}
