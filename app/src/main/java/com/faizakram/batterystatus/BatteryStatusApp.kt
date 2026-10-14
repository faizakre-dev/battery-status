package com.faizakram.batterystatus

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.isCharging
import com.faizakram.batterystatus.service.ChargeTrackingService
import com.faizakram.batterystatus.sync.SnapshotScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BatteryStatusApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var batteryInfoSource: BatteryInfoSource

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SnapshotScheduler.schedule(this)

        if (batteryInfoSource.current().isCharging) {
            runCatching {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, ChargeTrackingService::class.java),
                )
            }
        }
    }
}
