package com.faizakram.batterystatus.data.repo

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.HourlySnapshot
import kotlinx.coroutines.flow.Flow

/** App-facing gateway for persisting and observing battery data. */
interface BatteryRepository {

    suspend fun recordSnapshot(snapshot: BatterySnapshot): Long

    suspend fun recordSession(session: ChargeSession): Long

    fun observeLatestSnapshot(): Flow<BatterySnapshot?>

    fun observeSnapshotsSince(sinceMillis: Long): Flow<List<BatterySnapshot>>

    suspend fun snapshotsBefore(cutoffMillis: Long): List<BatterySnapshot>

    fun observeSessions(): Flow<List<ChargeSession>>

    suspend fun snapshotCount(): Int

    suspend fun trimSnapshotsBefore(cutoffMillis: Long): Int

    fun observeHourlySnapshotsSince(sinceMillis: Long): Flow<List<HourlySnapshot>>

    suspend fun saveHourlySnapshots(buckets: List<HourlySnapshot>): Int

    suspend fun hourlySnapshotCount(): Int

    fun observeEstimates(): Flow<BatteryEstimates?>

    suspend fun getEstimates(): BatteryEstimates?

    suspend fun saveEstimates(estimates: BatteryEstimates): Long
}
