package com.faizakram.batterystatus.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Owns scheduling of the background snapshot sampler. */
object SnapshotScheduler {

    private const val PERIODIC_SNAPSHOT_WORK = "periodic_snapshot"
    private val MIN_INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PeriodicSnapshotWorker>(
            MIN_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SNAPSHOT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
