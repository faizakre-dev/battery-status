package com.faizakram.batterystatus.core.model

enum class BatteryStatus {
    UNKNOWN,
    CHARGING,
    DISCHARGING,
    NOT_CHARGING,
    FULL,
}

enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
}

enum class PlugType {
    NONE,
    AC,
    USB,
    WIRELESS,
    DOCK,
}

data class BatterySnapshot(
    val timestampMillis: Long,
    val levelPercent: Int?,
    val scale: Int,
    val status: BatteryStatus,
    val health: BatteryHealth,
    val plugged: PlugType,
    val voltageMv: Int?,
    val temperatureCelsius: Float?,
    val technology: String?,
    val chargeCounterUah: Int?,
    val currentNowUa: Int?,
    val currentAvgUa: Int?,
    val capacityPercent: Int?,
    val energyCounterUah: Int?,
)

/** True when the device is connected to a power source or actively charging. */
val BatterySnapshot.isCharging: Boolean
    get() = plugged != PlugType.NONE ||
        status == BatteryStatus.CHARGING ||
        status == BatteryStatus.FULL
