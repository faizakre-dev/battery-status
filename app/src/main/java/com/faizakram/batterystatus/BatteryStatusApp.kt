package com.faizakram.batterystatus

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.faizakram.batterystatus.receiver.PowerConnectionHandler
import com.faizakram.batterystatus.sync.SnapshotScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class BatteryStatusApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var powerConnectionHandler: PowerConnectionHandler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        runCatching { SnapshotScheduler.schedule(this) }

        // Records the current reading, opens a session if charging (and starts
        // the tracking service), or closes a stale session left over from a
        // missed unplug broadcast.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { powerConnectionHandler.handle(this@BatteryStatusApp) }
        }
    }
}
