package com.faizakram.batterystatus.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.faizakram.batterystatus.MainActivity
import com.faizakram.batterystatus.R
import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.domain.alerts.BatteryAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Posts battery alerts to a dedicated notification channel. */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_alerts_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts [alert]. Silently no-ops when notifications are disabled or the
     * permission is missing, so alerting can never crash the tracking service.
     */
    @SuppressLint("MissingPermission")
    fun notify(alert: BatteryAlert, snapshot: BatterySnapshot) {
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return

        val (title, text) = content(alert, snapshot)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        runCatching { compat.notify(notificationId(alert), notification) }
    }

    private fun content(alert: BatteryAlert, snapshot: BatterySnapshot): Pair<String, String> =
        when (alert) {
            BatteryAlert.FULL_CHARGE -> context.getString(R.string.alert_full_title) to
                context.getString(R.string.alert_full_text)

            BatteryAlert.OVERHEAT -> context.getString(R.string.alert_overheat_title) to
                context.getString(
                    R.string.alert_overheat_text,
                    BatteryFormatters.temperature(snapshot.temperatureCelsius) ?: "—",
                )
        }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notificationId(alert: BatteryAlert): Int = ALERT_BASE_ID + alert.ordinal

    companion object {
        const val CHANNEL_ID = "battery_alerts"
        private const val ALERT_BASE_ID = 2000
    }
}
