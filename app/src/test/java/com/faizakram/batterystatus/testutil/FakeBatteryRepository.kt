package com.faizakram.batterystatus.testutil

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.HourlySnapshot
import com.faizakram.batterystatus.core.model.OpenChargeSession
import com.faizakram.batterystatus.data.repo.BatteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory [BatteryRepository] for tests, backed by plain lists. */
class FakeBatteryRepository : BatteryRepository {

    var open: OpenChargeSession? = null
        private set
    val sessions = mutableListOf<ChargeSession>()
    val snapshots = mutableListOf<BatterySnapshot>()

    override suspend fun getOpenSession(): OpenChargeSession? = open

    override suspend fun saveOpenSession(session: OpenChargeSession) {
        open = session
    }

    override suspend fun clearOpenSession() {
        open = null
    }

    override suspend fun recordSession(session: ChargeSession): Long {
        sessions += session
        return sessions.size.toLong()
    }

    override suspend fun recordSnapshot(snapshot: BatterySnapshot): Long {
        snapshots += snapshot
        return snapshots.size.toLong()
    }

    override fun observeLatestSnapshot(): Flow<BatterySnapshot?> = flowOf(snapshots.lastOrNull())
    override fun observeSnapshotsSince(sinceMillis: Long): Flow<List<BatterySnapshot>> = flowOf(snapshots)
    override suspend fun snapshotsBefore(cutoffMillis: Long): List<BatterySnapshot> = emptyList()
    override fun observeSessions(): Flow<List<ChargeSession>> = flowOf(sessions.toList())
    override suspend fun snapshotCount(): Int = snapshots.size
    override suspend fun trimSnapshotsBefore(cutoffMillis: Long): Int = 0
    override fun observeHourlySnapshotsSince(sinceMillis: Long): Flow<List<HourlySnapshot>> = flowOf(emptyList())
    override suspend fun saveHourlySnapshots(buckets: List<HourlySnapshot>): Int = 0
    override suspend fun hourlySnapshotCount(): Int = 0
    override fun observeEstimates(): Flow<BatteryEstimates?> = flowOf(null)
    override suspend fun getEstimates(): BatteryEstimates? = null
    override suspend fun saveEstimates(estimates: BatteryEstimates): Long = 0
}
