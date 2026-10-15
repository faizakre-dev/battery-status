package com.faizakram.batterystatus.session

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.OpenChargeSession
import com.faizakram.batterystatus.core.model.isCharging
import com.faizakram.batterystatus.data.repo.BatteryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single, persistence-backed entry point for charge-session bookkeeping.
 *
 * Snapshots arrive from three independent places — the charge-tracking
 * foreground service, the periodic WorkManager snapshot, and the
 * power-connection broadcast receiver — so all three share one in-flight
 * session stored in the database. That way a session survives process death and
 * the Android 12+ background foreground-service start restriction, and the
 * first non-charging snapshot from any source closes it exactly once.
 */
@Singleton
class ChargeSessionRecorder @Inject constructor(
    private val repository: BatteryRepository,
) {

    private val mutex = Mutex()

    /**
     * Feeds one snapshot into the in-flight session.
     *
     * @return the just-closed [ChargeSession] when this snapshot ended a
     * charge, or null while charging (or when there was no open session).
     */
    suspend fun onSnapshot(snapshot: BatterySnapshot): ChargeSession? =
        onSnapshot(snapshot, snapshot.isCharging)

    /** Same as [onSnapshot] with an explicit charging flag for stale broadcasts. */
    suspend fun onSnapshot(snapshot: BatterySnapshot, isCharging: Boolean): ChargeSession? =
        mutex.withLock {
            if (isCharging) {
                val open = repository.getOpenSession()
                val next = open?.accumulate(snapshot) ?: OpenChargeSession.start(snapshot)
                repository.saveOpenSession(next)
                null
            } else {
                val open = repository.getOpenSession() ?: return@withLock null
                val session = open.close(snapshot)
                repository.recordSession(session)
                repository.clearOpenSession()
                session
            }
        }
}
