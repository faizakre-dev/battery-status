package com.faizakram.batterystatus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faizakram.batterystatus.data.local.entity.BatteryEstimatesEntity
import com.faizakram.batterystatus.data.local.entity.BatterySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.ChargeSessionEntity
import com.faizakram.batterystatus.data.local.entity.HourlySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.OpenChargeSessionEntity
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

    @Query(
        """
        SELECT * FROM battery_snapshots
        WHERE timestampMillis < :cutoffMillis
        ORDER BY timestampMillis ASC
        """,
    )
    suspend fun getBefore(cutoffMillis: Long): List<BatterySnapshotEntity>

    @Query("DELETE FROM battery_snapshots WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long): Int
}

@Dao
interface HourlySnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(buckets: List<HourlySnapshotEntity>): List<Long>

    @Query(
        """
        SELECT * FROM battery_snapshots_hourly
        WHERE bucketStartMillis >= :sinceMillis
        ORDER BY bucketStartMillis ASC
        """,
    )
    fun observeSince(sinceMillis: Long): Flow<List<HourlySnapshotEntity>>

    @Query("SELECT COUNT(*) FROM battery_snapshots_hourly")
    suspend fun count(): Int
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

/** Reads and writes the single in-flight charge-session row. */
@Dao
interface OpenChargeSessionDao {

    @Query("SELECT * FROM open_charge_session WHERE id = :id")
    suspend fun get(id: Int): OpenChargeSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OpenChargeSessionEntity)

    @Query("DELETE FROM open_charge_session")
    suspend fun clear()
}

@Dao
interface BatteryEstimatesDao {

    @Query("SELECT * FROM battery_estimates WHERE id = :id")
    fun observe(id: Int): Flow<BatteryEstimatesEntity?>

    @Query("SELECT * FROM battery_estimates WHERE id = :id")
    suspend fun get(id: Int): BatteryEstimatesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BatteryEstimatesEntity): Long
}
