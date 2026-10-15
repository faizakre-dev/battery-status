package com.faizakram.batterystatus.receiver

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.isCharging
import com.faizakram.batterystatus.data.repo.BatteryRepository
import com.faizakram.batterystatus.service.ChargeTrackingService
import com.faizakram.batterystatus.session.ChargeSessionRecorder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles a plug-state change: records the reading, opens or closes the
 * persisted charge session, and (best effort) starts the tracking service.
 *
 * Separated from [PowerConnectionReceiver] so the behaviour under test is the
 * logic, not the broadcast plumbing. [isCharging] is taken from the broadcast
 * action rather than the sticky snapshot, which can lag the transition.
 */
@Singleton
class PowerConnectionHandler @Inject constructor(
    private val batteryInfoSource: BatteryInfoSource,
    private val repository: BatteryRepository,
    private val recorder: ChargeSessionRecorder,
) {

    suspend fun handle(
        context: Context,
        snapshot: BatterySnapshot = batteryInfoSource.current(),
        isCharging: Boolean = snapshot.isCharging,
    ) {
        repository.recordSnapshot(snapshot)
        recorder.onSnapshot(snapshot, isCharging)

        if (isCharging) {
            // Background foreground-service starts are restricted on Android 12+;
            // the session itself is already persisted by [recorder] above, so a
            // failure here only skips the higher-frequency sampling.
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ChargeTrackingService::class.java),
                )
            }
        }
    }
}
