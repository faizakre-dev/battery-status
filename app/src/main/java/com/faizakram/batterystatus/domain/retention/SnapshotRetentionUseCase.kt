package com.faizakram.batterystatus.domain.retention

import com.faizakram.batterystatus.data.repo.BatteryRepository
import javax.inject.Inject

/**
 * Compresses raw snapshots older than [SnapshotRetention.RAW_RETENTION_MILLIS]
 * into hourly aggregates, then deletes the raw rows. Idempotent: re-running with
 * no old snapshots is a no-op.
 */
class SnapshotRetentionUseCase @Inject constructor(
    private val repository: BatteryRepository,
) {

    suspend fun run(nowMillis: Long) {
        val cutoff = nowMillis - SnapshotRetention.RAW_RETENTION_MILLIS
        val stale = repository.snapshotsBefore(cutoff)
        if (stale.isEmpty()) return

        val buckets = SnapshotRetention.aggregateHourly(stale)
        repository.saveHourlySnapshots(buckets)
        repository.trimSnapshotsBefore(cutoff)
    }
}
