package com.faizakram.batterystatus.core.battery

import android.os.BatteryManager
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType

fun batteryStatusFrom(raw: Int): BatteryStatus = when (raw) {
    BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
    BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
    BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
    else -> BatteryStatus.UNKNOWN
}

fun batteryHealthFrom(raw: Int): BatteryHealth = when (raw) {
    BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
    BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
    BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
    else -> BatteryHealth.UNKNOWN
}

fun plugTypeFrom(raw: Int): PlugType = when (raw) {
    BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
    BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
    BatteryManager.BATTERY_PLUGGED_DOCK -> PlugType.DOCK
    else -> PlugType.NONE
}

/**
 * Android reports temperature in tenths of a degree Celsius. A missing value is
 * reported as [Int.MIN_VALUE] by the platform.
 */
fun temperatureCelsius(rawTenths: Int): Float? =
    if (rawTenths == Int.MIN_VALUE) null else rawTenths / 10f
