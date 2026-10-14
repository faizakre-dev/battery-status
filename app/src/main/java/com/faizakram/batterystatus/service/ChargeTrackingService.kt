package com.faizakram.batterystatus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.faizakram.batterystatus.MainActivity
import com.faizakram.batterystatus.R
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.battery.ChargeSessionTracker
import com.faizakram.batterystatus.core.battery.TrackerEvent
import com.faizakram.batterystatus.core.model.isCharging
import com.faizakram.batterystatus.data.repo.BatteryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that samples the battery at a high cadence while the device
 * is plugged in, persists snapshots, and records the boundaries of a charge session.
 *
 * It stops itself as soon as a snapshot reports the device is no longer charging.
 */
@AndroidEntryPoint
class ChargeTrackingService : Service() {

    @Inject lateinit var batteryInfoSource: BatteryInfoSource
    @Inject lateinit var repository: BatteryRepository

    private val tracker = ChargeSessionTracker()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (collectJob == null) {
            collectJob = scope.launch { observeBattery() }
        }
        return START_STICKY
    }

    private suspend fun observeBattery() {
        batteryInfoSource.snapshots.collect { snapshot ->
            repository.recordSnapshot(snapshot)
            tracker.onSnapshot(snapshot).forEach { event ->
                if (event is TrackerEvent.SessionClosed) {
                    repository.recordSession(event.session)
                }
            }
            if (!snapshot.isCharging) {
                stopSelf()
            }
        }
    }

    private fun startAsForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    override fun onDestroy() {
        collectJob = null
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_tracking),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_tracking_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(getString(R.string.notification_tracking_text))
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "charge_tracking"
        private const val NOTIFICATION_ID = 1001
    }
}
