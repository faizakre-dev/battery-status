package com.faizakram.batterystatus.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Single source of truth for live battery data exposed by the platform.
 * Implementations wrap [BatteryManager] (and later, best-effort sysfs reads).
 */
interface BatteryInfoSource {
    /** Emits a new [BatterySnapshot] on every `ACTION_BATTERY_CHANGED` broadcast. */
    val snapshots: Flow<BatterySnapshot>

    /** Reads the current sticky broadcast synchronously. */
    fun current(): BatterySnapshot
}

class AndroidBatteryInfoSource(
    private val context: Context,
) : BatteryInfoSource {

    private val batteryManager: BatteryManager?
        get() = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    override val snapshots: Flow<BatterySnapshot> = callbackFlow {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        context.registerReceiver(null, filter)?.let { trySend(it.toSnapshot()) }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                intent?.let { trySend(it.toSnapshot()) }
            }
        }
        context.registerReceiver(receiver, filter)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    override fun current(): BatterySnapshot =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.toSnapshot()
            ?: emptySnapshot()

    private fun Intent.toSnapshot(): BatterySnapshot = BatterySnapshot(
        timestampMillis = System.currentTimeMillis(),
        levelPercent = getIntExtra(BatteryManager.EXTRA_LEVEL, -1).takeIf { it in 0..100 },
        scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        status = batteryStatusFrom(
            getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
        ),
        health = batteryHealthFrom(
            getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
        ),
        plugged = plugTypeFrom(getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)),
        voltageMv = getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 },
        temperatureCelsius = temperatureCelsius(
            getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE),
        ),
        technology = getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
        chargeCounterUah = intProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
        currentNowUa = intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
        currentAvgUa = intProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
        capacityPercent = intProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 },
        energyCounterUah = intProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
    )

    private fun intProperty(id: Int): Int? =
        batteryManager?.getIntProperty(id)?.takeIf { it != Int.MIN_VALUE }

    private fun emptySnapshot(): BatterySnapshot = BatterySnapshot(
        timestampMillis = System.currentTimeMillis(),
        levelPercent = null,
        scale = -1,
        status = BatteryStatus.UNKNOWN,
        health = BatteryHealth.UNKNOWN,
        plugged = PlugType.NONE,
        voltageMv = null,
        temperatureCelsius = null,
        technology = null,
        chargeCounterUah = null,
        currentNowUa = null,
        currentAvgUa = null,
        capacityPercent = null,
        energyCounterUah = null,
    )
}
