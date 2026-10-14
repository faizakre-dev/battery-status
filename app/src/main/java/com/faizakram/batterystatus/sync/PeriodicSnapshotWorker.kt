package com.faizakram.batterystatus.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.data.repo.BatteryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Low-frequency background sampling while the device is idle. Charged/active
 * periods are captured at a higher cadence by [ChargeTrackingService].
 */
@HiltWorker
class PeriodicSnapshotWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryInfoSource: BatteryInfoSource,
    private val repository: BatteryRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        repository.recordSnapshot(batteryInfoSource.current())
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
