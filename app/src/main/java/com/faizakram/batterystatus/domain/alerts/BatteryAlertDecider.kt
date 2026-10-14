package com.faizakram.batterystatus.domain.alerts

import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.isCharging

/**
 * Stateful, pure decision logic for battery alerts. Emits each alert at most
 * once per episode:
 *
 * - [BatteryAlert.OVERHEAT] fires when temperature crosses [overheatCelsius] and
 *   re-arms only after it falls back to [clearCelsius] (hysteresis, avoids flapping).
 * - [BatteryAlert.FULL_CHARGE] fires once when the battery reports [BatteryStatus.FULL]
 *   and re-arms after the device is unplugged.
 *
 * No Android dependencies, so it is unit-testable directly.
 */
class BatteryAlertDecider(
    private val overheatCelsius: Float = DEFAULT_OVERHEAT_CELSIUS,
    private val clearCelsius: Float = DEFAULT_CLEAR_CELSIUS,
) {
    private var overheatActive = false
    private var fullChargeNotified = false

    /** Returns the alerts triggered by [snapshot] (usually empty). */
    fun onSnapshot(snapshot: BatterySnapshot): List<BatteryAlert> {
        val alerts = mutableListOf<BatteryAlert>()
        val temperature = snapshot.temperatureCelsius

        when {
            !overheatActive && temperature != null && temperature >= overheatCelsius -> {
                overheatActive = true
                alerts += BatteryAlert.OVERHEAT
            }
            overheatActive && (temperature == null || temperature <= clearCelsius) -> {
                overheatActive = false
            }
        }

        if (!snapshot.isCharging) {
            fullChargeNotified = false
        } else if (!fullChargeNotified && snapshot.status == BatteryStatus.FULL) {
            fullChargeNotified = true
            alerts += BatteryAlert.FULL_CHARGE
        }

        return alerts
    }

    fun reset() {
        overheatActive = false
        fullChargeNotified = false
    }

    companion object {
        const val DEFAULT_OVERHEAT_CELSIUS = 45f
        const val DEFAULT_CLEAR_CELSIUS = 42f
    }
}
