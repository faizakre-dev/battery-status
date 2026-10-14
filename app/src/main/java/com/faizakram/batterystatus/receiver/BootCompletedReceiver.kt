package com.faizakram.batterystatus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.faizakram.batterystatus.service.ChargeTrackingService

/** Resumes charge-session tracking after a reboot if the device is plugged in. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return

        if (batteryManager.isCharging) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ChargeTrackingService::class.java),
                )
            }
        }
    }
}
