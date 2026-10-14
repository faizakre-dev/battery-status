package com.faizakram.batterystatus.domain.stats

/**
 * A single observed battery sample used for rate estimation.
 *
 * [chargeCounterUah] is the remaining charge; its sign and unit convention can
 * vary per OEM, so callers should pass it as reported by `BatteryManager`.
 */
data class ChargeSample(
    val timestampMillis: Long,
    val chargeCounterUah: Int?,
    val isCharging: Boolean,
)

/**
 * Tracks the smoothed charge-counter rate using an exponential weighted moving
 * average and converts it into time-to-full / time-remaining estimates.
 *
 * Sign convention: a **positive** rate means charge is rising (charging);
 * a **negative** rate means charge is falling (discharging). The estimator
 * resets its rate series whenever [ChargeSample.isCharging] flips, so a charge
 * and a discharge never blend into one bogus rate.
 */
class TimeToFullEstimator(
    private val alpha: Double = DEFAULT_ALPHA,
    private val maxGapMillis: Long = DEFAULT_MAX_GAP_MILLIS,
) {
    private var previous: ChargeSample? = null
    private var smoothedRateUaPerMs: Double? = null

    /**
     * Feeds the next sample and returns the current smoothed charge-counter
     * rate in microamp-hours per millisecond, or `null` if it is not yet known.
     */
    fun onSample(sample: ChargeSample): Double? {
        val previousSample = previous
        previous = sample

        // A charging-state flip invalidates the current series.
        if (previousSample == null || previousSample.isCharging != sample.isCharging) {
            smoothedRateUaPerMs = null
            return null
        }

        val previousCounter = previousSample.chargeCounterUah ?: return smoothedRateUaPerMs
        val currentCounter = sample.chargeCounterUah ?: return smoothedRateUaPerMs
        val gapMillis = sample.timestampMillis - previousSample.timestampMillis
        if (gapMillis <= 0 || gapMillis > maxGapMillis) return smoothedRateUaPerMs

        val instantaneous = (currentCounter - previousCounter).toDouble() / gapMillis
        val previousRate = smoothedRateUaPerMs
        val newRate = if (previousRate == null) {
            instantaneous
        } else {
            alpha * instantaneous + (1 - alpha) * previousRate
        }
        smoothedRateUaPerMs = newRate
        return newRate
    }

    fun reset() {
        previous = null
        smoothedRateUaPerMs = null
    }

    companion object {
        private const val DEFAULT_ALPHA = 0.4
        private const val DEFAULT_MAX_GAP_MILLIS = 5 * 60_000L

        /** Milliseconds until full while charging, or `null` when not charging/unknown. */
        fun timeToFullMillis(
            fullCapacityUah: Int?,
            nowUah: Int?,
            rateUaPerMs: Double?,
        ): Long? {
            if (rateUaPerMs == null || rateUaPerMs <= MIN_RATE_UA_PER_MS) return null
            val full = fullCapacityUah?.takeIf { it > 0 } ?: return null
            val now = nowUah?.takeIf { it >= 0 } ?: return null
            val remaining = (full - now).coerceAtLeast(0)
            return (remaining / rateUaPerMs).toLong()
        }

        /** Milliseconds until empty while discharging, or `null` when not discharging/unknown. */
        fun remainingMillis(
            nowUah: Int?,
            rateUaPerMs: Double?,
        ): Long? {
            if (rateUaPerMs == null || rateUaPerMs >= -MIN_RATE_UA_PER_MS) return null
            val now = nowUah?.takeIf { it > 0 } ?: return null
            return (now / -rateUaPerMs).toLong()
        }

        private const val MIN_RATE_UA_PER_MS = 1e-6
    }
}
