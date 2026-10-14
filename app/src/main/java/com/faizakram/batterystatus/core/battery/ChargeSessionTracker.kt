package com.faizakram.batterystatus.core.battery

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.isCharging

/** Events emitted by [ChargeSessionTracker] as snapshots are fed in. */
sealed interface TrackerEvent {
    data class SessionStarted(val timestampMillis: Long) : TrackerEvent
    data class SessionClosed(val session: ChargeSession) : TrackerEvent
}

/**
 * Detects charging sessions from a stream of [BatterySnapshot]s.
 *
 * A session opens on the first charging snapshot and closes on the first
 * non-charging snapshot. This class is pure Kotlin so it can be unit tested
 * without Android; the owning process (a foreground service) is responsible for
 * persisting the produced [ChargeSession].
 */
class ChargeSessionTracker {

    private var openSession: OpenSession? = null

    val hasOpenSession: Boolean
        get() = openSession != null

    fun onSnapshot(snapshot: BatterySnapshot): List<TrackerEvent> {
        val events = mutableListOf<TrackerEvent>()
        if (snapshot.isCharging) {
            val current = openSession
            if (current == null) {
                openSession = OpenSession.start(snapshot)
                events += TrackerEvent.SessionStarted(snapshot.timestampMillis)
            } else {
                openSession = current.accumulate(snapshot)
            }
        } else {
            openSession?.let {
                openSession = null
                events += TrackerEvent.SessionClosed(it.close(snapshot))
            }
        }
        return events
    }

    fun reset() {
        openSession = null
    }

    private class OpenSession(
        val startMillis: Long,
        val percentFrom: Int?,
        val startChargeCounterUah: Int?,
        val lastChargeCounterUah: Int?,
        val maxPercent: Int?,
        val maxTemperatureCelsius: Float?,
        val currentSumUa: Long,
        val currentSamples: Int,
        val reachedFull: Boolean,
    ) {
        fun accumulate(snapshot: BatterySnapshot): OpenSession = OpenSession(
            startMillis = startMillis,
            percentFrom = percentFrom,
            startChargeCounterUah = startChargeCounterUah,
            lastChargeCounterUah = snapshot.chargeCounterUah ?: lastChargeCounterUah,
            maxPercent = maxOfNullable(maxPercent, snapshot.levelPercent),
            maxTemperatureCelsius = maxOfNullable(maxTemperatureCelsius, snapshot.temperatureCelsius),
            currentSumUa = currentSumUa + (snapshot.currentNowUa?.toLong() ?: 0L),
            currentSamples = currentSamples + if (snapshot.currentNowUa != null) 1 else 0,
            reachedFull = reachedFull || snapshot.isFull(),
        )

        fun close(final: BatterySnapshot): ChargeSession {
            val endCounter = final.chargeCounterUah ?: lastChargeCounterUah
            val mahAdded = if (startChargeCounterUah != null && endCounter != null) {
                (endCounter - startChargeCounterUah).coerceAtLeast(0) / UAH_PER_MAH
            } else {
                null
            }
            return ChargeSession(
                startMillis = startMillis,
                endMillis = final.timestampMillis,
                percentFrom = percentFrom,
                percentTo = maxOfNullable(maxPercent, final.levelPercent),
                mahAdded = mahAdded,
                maxTemperatureCelsius = maxOfNullable(maxTemperatureCelsius, final.temperatureCelsius),
                avgCurrentUa = if (currentSamples > 0) (currentSumUa / currentSamples).toInt() else null,
                isFull = reachedFull || final.isFull(),
            )
        }

        companion object {
            fun start(snapshot: BatterySnapshot): OpenSession = OpenSession(
                startMillis = snapshot.timestampMillis,
                percentFrom = snapshot.levelPercent,
                startChargeCounterUah = snapshot.chargeCounterUah,
                lastChargeCounterUah = snapshot.chargeCounterUah,
                maxPercent = snapshot.levelPercent,
                maxTemperatureCelsius = snapshot.temperatureCelsius,
                currentSumUa = snapshot.currentNowUa?.toLong() ?: 0L,
                currentSamples = if (snapshot.currentNowUa != null) 1 else 0,
                reachedFull = snapshot.isFull(),
            )
        }
    }

}

private const val UAH_PER_MAH = 1000

private fun BatterySnapshot.isFull(): Boolean =
    status == BatteryStatus.FULL || levelPercent == 100

private fun <T : Comparable<T>> maxOfNullable(a: T?, b: T?): T? = when {
    a == null -> b
    b == null -> a
    else -> maxOf(a, b)
}
