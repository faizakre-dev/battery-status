package com.faizakram.batterystatus.testutil

import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType

/** Builds a [BatterySnapshot] with sensible defaults for tests. */
fun testSnapshot(
    ts: Long,
    percent: Int? = 50,
    status: BatteryStatus = BatteryStatus.DISCHARGING,
    plugged: PlugType = PlugType.NONE,
    temperatureCelsius: Float? = 30f,
    currentNowUa: Int? = null,
    chargeCounterUah: Int? = null,
): BatterySnapshot = BatterySnapshot(
    timestampMillis = ts,
    levelPercent = percent,
    scale = 100,
    status = status,
    health = BatteryHealth.GOOD,
    plugged = plugged,
    voltageMv = 4_200,
    temperatureCelsius = temperatureCelsius,
    technology = "Li-ion",
    chargeCounterUah = chargeCounterUah,
    currentNowUa = currentNowUa,
    currentAvgUa = null,
    capacityPercent = null,
    energyCounterUah = null,
)

fun chargingSnapshot(
    ts: Long,
    percent: Int? = 50,
    temperatureCelsius: Float? = 30f,
    currentNowUa: Int? = null,
    chargeCounterUah: Int? = null,
): BatterySnapshot = testSnapshot(
    ts = ts,
    percent = percent,
    status = BatteryStatus.CHARGING,
    plugged = PlugType.USB,
    temperatureCelsius = temperatureCelsius,
    currentNowUa = currentNowUa,
    chargeCounterUah = chargeCounterUah,
)

fun unpluggedSnapshot(
    ts: Long,
    percent: Int? = 50,
    temperatureCelsius: Float? = 30f,
    currentNowUa: Int? = null,
    chargeCounterUah: Int? = null,
): BatterySnapshot = testSnapshot(
    ts = ts,
    percent = percent,
    status = BatteryStatus.DISCHARGING,
    plugged = PlugType.NONE,
    temperatureCelsius = temperatureCelsius,
    currentNowUa = currentNowUa,
    chargeCounterUah = chargeCounterUah,
)
