package com.faizakram.batterystatus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.faizakram.batterystatus.data.local.entity.BatterySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.ChargeSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySnapshotDao {

    @Insert
    suspend fun insert(snapshot: BatterySnapshotEntity): Long

    @Query("SELECT * FROM battery_snapshots ORDER BY timestampMillis DESC LIMIT 1")
    fun observeLatest(): Flow<BatterySnapshotEntity?>

    @Query(
        """
        SELECT * FROM battery_snapshots
        WHERE timestampMillis >= :sinceMillis
        ORDER BY timestampMillis ASC
        """,
    )
    fun observeSince(sinceMillis: Long): Flow<List<BatterySnapshotEntity>>

    @Query("SELECT COUNT(*) FROM battery_snapshots")
    suspend fun count(): Int

    @Query("DELETE FROM battery_snapshots WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long): Int
}

@Dao
interface ChargeSessionDao {

    @Insert
    suspend fun insert(session: ChargeSessionEntity): Long

    @Query("SELECT * FROM charge_sessions ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<ChargeSessionEntity>>

    @Query("SELECT * FROM charge_sessions ORDER BY startMillis DESC LIMIT 1")
    suspend fun latest(): ChargeSessionEntity?

    @Query("SELECT * FROM charge_sessions WHERE isFull = 1 ORDER BY startMillis DESC")
    suspend fun fullChargeSessions(): List<ChargeSessionEntity>

    @Query("SELECT COUNT(*) FROM charge_sessions")
    suspend fun count(): Int
}
