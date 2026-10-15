package com.faizakram.batterystatus.core.model

/**
 * A charge session that is still in progress.
 *
 * The tracker accumulates snapshots into this record, which is persisted by the
 * repository so a session survives process death and foreground-service start
 * restrictions. When the device unplugs, [close] produces the final
 * [ChargeSession].
 */
data class OpenChargeSession(
    val startMillis: Long,
    val lastMillis: Long,
    val percentFrom: Int?,
    val startChargeCounterUah: Int?,
    val lastChargeCounterUah: Int?,
    val maxPercent: Int?,
    val maxTemperatureCelsius: Float?,
    val currentSumUa: Long,
    val currentSamples: Int,
    val reachedFull: Boolean,
) {

    fun accumulate(snapshot: BatterySnapshot): OpenChargeSession = copy(
        lastMillis = snapshot.timestampMillis,
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
            // End at the last observed charging sample, so a session still has
            // the right duration when the unplug broadcast was missed and the
            // close happens much later.
            endMillis = lastMillis,
            percentFrom = percentFrom,
            percentTo = maxOfNullable(maxPercent, final.levelPercent),
            mahAdded = mahAdded,
            maxTemperatureCelsius = maxOfNullable(maxTemperatureCelsius, final.temperatureCelsius),
            avgCurrentUa = if (currentSamples > 0) (currentSumUa / currentSamples).toInt() else null,
            isFull = reachedFull || final.isFull(),
        )
    }

    companion object {
        fun start(snapshot: BatterySnapshot): OpenChargeSession = OpenChargeSession(
            startMillis = snapshot.timestampMillis,
            lastMillis = snapshot.timestampMillis,
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

private const val UAH_PER_MAH = 1000

private fun BatterySnapshot.isFull(): Boolean =
    status == BatteryStatus.FULL || levelPercent == 100

private fun <T : Comparable<T>> maxOfNullable(a: T?, b: T?): T? = when {
    a == null -> b
    b == null -> a
    else -> maxOf(a, b)
}
