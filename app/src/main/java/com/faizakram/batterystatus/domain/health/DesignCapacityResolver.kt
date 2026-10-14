package com.faizakram.batterystatus.domain.health

import kotlin.math.roundToInt

/**
 * Resolves the design capacity used for cycle and health math.
 *
 * Preference order:
 * 1. sysfs `charge_full_design` (real value when readable),
 * 2. the design capacity already stored from a previous read,
 * 3. a conservative fallback of 1.1x the largest observed charge counter.
 */
object DesignCapacityResolver {

    private const val FALLBACK_FACTOR = 1.1

    fun resolve(
        sysfsDesignUah: Int?,
        currentDesignUah: Int?,
        lifetimePeakUah: Int?,
    ): Int? = sysfsDesignUah?.takeIf { it > 0 }
        ?: currentDesignUah?.takeIf { it > 0 }
        ?: lifetimePeakUah?.takeIf { it > 0 }?.let { (it * FALLBACK_FACTOR).roundToInt() }
}
