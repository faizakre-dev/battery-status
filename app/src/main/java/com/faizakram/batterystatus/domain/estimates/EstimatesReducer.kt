package com.faizakram.batterystatus.domain.estimates

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryData
import com.faizakram.batterystatus.domain.cycle.CycleEstimator
import com.faizakram.batterystatus.domain.health.DesignCapacityResolver

/** Inputs describing what happened since the stored estimates were last updated. */
data class EstimateUpdate(
    val session: ChargeSession? = null,
    val observedChargeCounterUah: Int? = null,
    val sysfs: SysfsBatteryData? = null,
    val nowMillis: Long,
)

/**
 * Pure reducer that folds a new [EstimateUpdate] into the stored
 * [BatteryEstimates]. No I/O; callers persist the result if it changed.
 */
object EstimatesReducer {

    fun reduce(current: BatteryEstimates, update: EstimateUpdate): BatteryEstimates {
        val lifetimePeakUah = maxOfNullable(current.lifetimePeakUah, update.observedChargeCounterUah)
        val designCapacityUah = DesignCapacityResolver.resolve(
            sysfsDesignUah = update.sysfs?.chargeFullDesignUah,
            currentDesignUah = current.designCapacityUah,
            lifetimePeakUah = lifetimePeakUah,
        )
        val session = update.session
        val accumulated = if (session != null) {
            current.accumulatedCycleFraction +
                CycleEstimator.sessionContribution(session.mahAdded, designCapacityUah)
        } else {
            current.accumulatedCycleFraction
        }

        return BatteryEstimates(
            designCapacityUah = designCapacityUah,
            lifetimePeakUah = lifetimePeakUah,
            accumulatedCycleFraction = accumulated,
            sysfsCycleCount = update.sysfs?.cycleCount ?: current.sysfsCycleCount,
            lastUpdatedMillis = update.nowMillis,
        )
    }

    private fun maxOfNullable(a: Int?, b: Int?): Int? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }
}
