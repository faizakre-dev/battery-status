package com.faizakram.batterystatus.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryReader
import com.faizakram.batterystatus.data.repo.BatteryRepository
import com.faizakram.batterystatus.domain.estimates.EstimateUpdate
import com.faizakram.batterystatus.domain.estimates.EstimatesReducer
import com.faizakram.batterystatus.domain.retention.SnapshotRetentionUseCase
import com.faizakram.batterystatus.session.ChargeSessionRecorder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Low-frequency background sampling while the device is idle. Charged/active
 * periods are captured at a higher cadence by [ChargeSessionRecorder] via the
 * charge-tracking foreground service, but the recorder is also fed here so a
 * session still opens and closes when that service cannot run (for example
 * while the app is in the background on Android 12+).
 */
@HiltWorker
class PeriodicSnapshotWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryInfoSource: BatteryInfoSource,
    private val repository: BatteryRepository,
    private val sysfsBatteryReader: SysfsBatteryReader,
    private val snapshotRetention: SnapshotRetentionUseCase,
    private val chargeSessionRecorder: ChargeSessionRecorder,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        val snapshot = batteryInfoSource.current()
        repository.recordSnapshot(snapshot)
        chargeSessionRecorder.onSnapshot(snapshot)
        updateEstimates(snapshot)
        snapshotRetention.run(snapshot.timestampMillis)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    private suspend fun updateEstimates(snapshot: BatterySnapshot) {
        val current = repository.getEstimates() ?: BatteryEstimates.EMPTY
        val next = EstimatesReducer.reduce(
            current = current,
            update = EstimateUpdate(
                observedChargeCounterUah = snapshot.chargeCounterUah,
                sysfs = sysfsBatteryReader.read(),
                nowMillis = snapshot.timestampMillis,
            ),
        )
        if (next.copy(lastUpdatedMillis = 0L) != current.copy(lastUpdatedMillis = 0L)) {
            repository.saveEstimates(next)
        }
    }
}
