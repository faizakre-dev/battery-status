package com.faizakram.batterystatus.domain.health

import kotlin.math.roundToInt

/**
 * Battery health percentage: remaining capacity versus design capacity.
 * Works both for the sysfs case (`charge_full` vs `charge_full_design`) and
 * for the app's lifetime-peak fallback against the resolved design capacity.
 */
object HealthCalculator {

    /** health% = fullCharge / designCapacity * 100, clamped to 0..100. */
    fun healthPercent(fullChargeUah: Int?, designCapacityUah: Int?): Int? {
        val design = designCapacityUah?.takeIf { it > 0 } ?: return null
        val full = fullChargeUah?.takeIf { it > 0 } ?: return null
        return ((full.toDouble() / design) * 100).roundToInt().coerceIn(0, 100)
    }
}
