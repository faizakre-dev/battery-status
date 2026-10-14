package com.faizakram.batterystatus.data.repo

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import kotlinx.coroutines.flow.Flow

/** App-facing gateway for persisting and observing battery data. */
interface BatteryRepository {

    suspend fun recordSnapshot(snapshot: BatterySnapshot): Long

    suspend fun recordSession(session: ChargeSession): Long

    fun observeLatestSnapshot(): Flow<BatterySnapshot?>

    fun observeSnapshotsSince(sinceMillis: Long): Flow<List<BatterySnapshot>>

    fun observeSessions(): Flow<List<ChargeSession>>

    suspend fun snapshotCount(): Int

    suspend fun trimSnapshotsBefore(cutoffMillis: Long): Int
}
