package com.faizakram.batterystatus.domain.cycle

/**
 * Charge-cycle estimation without kernel data.
 *
 * A "cycle" is a cumulative 100% worth of charged energy, not one plug-in
 * event. Each completed session contributes `mAhAdded / designCapacity`.
 */
object CycleEstimator {

    /** Guard against absurd single-session values from flaky OEM counters. */
    private const val MAX_SESSION_CONTRIBUTION = 1.5

    /** Fraction of one cycle a single charging session represents. */
    fun sessionContribution(mahAdded: Int?, designCapacityUah: Int?): Double {
        val designMah = designCapacityUah?.takeIf { it > 0 }?.let { it / 1000.0 } ?: return 0.0
        val addedMah = mahAdded?.takeIf { it > 0 } ?: return 0.0
        return (addedMah / designMah).coerceAtMost(MAX_SESSION_CONTRIBUTION)
    }

    /**
     * Effective cycle count: the kernel value when readable, otherwise the
     * app-accumulated fraction.
     */
    fun effectiveCycles(accumulatedCycleFraction: Double, sysfsCycleCount: Int?): Double =
        if (sysfsCycleCount != null) sysfsCycleCount.toDouble()
        else accumulatedCycleFraction.coerceAtLeast(0.0)
}
