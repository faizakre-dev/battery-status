package com.faizakram.batterystatus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Starts and ends charge-session tracking when the charger is plugged in or
 * unplugged. This is the missing trigger: without it the foreground service
 * only ran if the app happened to be launched while already charging.
 *
 * The session itself is persisted by the recorder (see [PowerConnectionHandler]),
 * so the row is recorded even when the background foreground-service start is
 * blocked (Android 12+).
 */
@AndroidEntryPoint
class PowerConnectionReceiver : BroadcastReceiver() {

    @Inject lateinit var handler: PowerConnectionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val charging = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> true
            Intent.ACTION_POWER_DISCONNECTED -> false
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handler.handle(context, isCharging = charging)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
