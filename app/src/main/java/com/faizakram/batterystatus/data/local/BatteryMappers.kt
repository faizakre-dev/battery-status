package com.faizakram.batterystatus.data.local

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.model.HourlySnapshot
import com.faizakram.batterystatus.core.model.OpenChargeSession
import com.faizakram.batterystatus.data.local.entity.BatteryEstimatesEntity
import com.faizakram.batterystatus.data.local.entity.BatterySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.ChargeSessionEntity
import com.faizakram.batterystatus.data.local.entity.HourlySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.OpenChargeSessionEntity
import kotlin.math.roundToInt

private const val MICRO_TO_MILLI = 1_000_000L

fun BatterySnapshot.toEntity(): BatterySnapshotEntity = BatterySnapshotEntity(
    timestampMillis = timestampMillis,
    levelPercent = levelPercent,
    status = status,
    health = health,
    plugged = plugged,
    voltageMv = voltageMv,
    temperatureDeciCelsius = temperatureCelsius?.let { (it * 10).roundToInt() },
    currentUa = currentNowUa,
    chargeCounterUah = chargeCounterUah,
    powerMw = computePowerMw(),
)

/** Power in milliwatts derived from voltage and current, when both are known. */
private fun BatterySnapshot.computePowerMw(): Int? {
    val voltageMv = voltageMv ?: return null
    val currentUa = currentNowUa ?: return null
    val milliwatts = (voltageMv.toLong() * currentUa) / MICRO_TO_MILLI
    return milliwatts.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}

fun BatterySnapshotEntity.toDomain(): BatterySnapshot = BatterySnapshot(
    timestampMillis = timestampMillis,
    levelPercent = levelPercent,
    scale = -1,
    status = status,
    health = health,
    plugged = plugged,
    voltageMv = voltageMv,
    temperatureCelsius = temperatureDeciCelsius?.let { it / 10f },
    technology = null,
    chargeCounterUah = chargeCounterUah,
    currentNowUa = currentUa,
    currentAvgUa = null,
    capacityPercent = null,
    energyCounterUah = null,
)

fun ChargeSession.toEntity(): ChargeSessionEntity = ChargeSessionEntity(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    percentFrom = percentFrom,
    percentTo = percentTo,
    mahAdded = mahAdded,
    maxTemperatureDeciCelsius = maxTemperatureCelsius?.let { (it * 10).roundToInt() },
    avgCurrentUa = avgCurrentUa,
    isFull = isFull,
)

fun ChargeSessionEntity.toDomain(): ChargeSession = ChargeSession(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    percentFrom = percentFrom,
    percentTo = percentTo,
    mahAdded = mahAdded,
    maxTemperatureCelsius = maxTemperatureDeciCelsius?.let { it / 10f },
    avgCurrentUa = avgCurrentUa,
    isFull = isFull,
)

fun BatteryEstimates.toEntity(): BatteryEstimatesEntity = BatteryEstimatesEntity(
    id = BatteryEstimatesEntity.SINGLETON_ID,
    designCapacityUah = designCapacityUah,
    lifetimePeakUah = lifetimePeakUah,
    accumulatedCycleFraction = accumulatedCycleFraction,
    sysfsCycleCount = sysfsCycleCount,
    lastUpdatedMillis = lastUpdatedMillis,
)

fun BatteryEstimatesEntity.toDomain(): BatteryEstimates = BatteryEstimates(
    designCapacityUah = designCapacityUah,
    lifetimePeakUah = lifetimePeakUah,
    accumulatedCycleFraction = accumulatedCycleFraction,
    sysfsCycleCount = sysfsCycleCount,
    lastUpdatedMillis = lastUpdatedMillis,
)

fun HourlySnapshot.toEntity(): HourlySnapshotEntity = HourlySnapshotEntity(
    bucketStartMillis = bucketStartMillis,
    sampleCount = sampleCount,
    avgLevelPercent = avgLevelPercent,
    minLevelPercent = minLevelPercent,
    maxLevelPercent = maxLevelPercent,
    avgTemperatureDeciCelsius = avgTemperatureCelsius?.let { (it * 10).roundToInt().toFloat() },
    minTemperatureDeciCelsius = minTemperatureCelsius?.let { (it * 10).roundToInt() },
    maxTemperatureDeciCelsius = maxTemperatureCelsius?.let { (it * 10).roundToInt() },
)

fun HourlySnapshotEntity.toDomain(): HourlySnapshot = HourlySnapshot(
    bucketStartMillis = bucketStartMillis,
    sampleCount = sampleCount,
    avgLevelPercent = avgLevelPercent,
    minLevelPercent = minLevelPercent,
    maxLevelPercent = maxLevelPercent,
    avgTemperatureCelsius = avgTemperatureDeciCelsius?.let { it / 10f },
    minTemperatureCelsius = minTemperatureDeciCelsius?.let { it / 10f },
    maxTemperatureCelsius = maxTemperatureDeciCelsius?.let { it / 10f },
)

fun OpenChargeSession.toEntity(): OpenChargeSessionEntity = OpenChargeSessionEntity(
    id = OpenChargeSessionEntity.SINGLETON_ID,
    startMillis = startMillis,
    lastMillis = lastMillis,
    percentFrom = percentFrom,
    startChargeCounterUah = startChargeCounterUah,
    lastChargeCounterUah = lastChargeCounterUah,
    maxPercent = maxPercent,
    maxTemperatureDeciCelsius = maxTemperatureCelsius?.let { (it * 10).roundToInt() },
    currentSumUa = currentSumUa,
    currentSamples = currentSamples,
    reachedFull = reachedFull,
)

fun OpenChargeSessionEntity.toDomain(): OpenChargeSession = OpenChargeSession(
    startMillis = startMillis,
    lastMillis = lastMillis,
    percentFrom = percentFrom,
    startChargeCounterUah = startChargeCounterUah,
    lastChargeCounterUah = lastChargeCounterUah,
    maxPercent = maxPercent,
    maxTemperatureCelsius = maxTemperatureDeciCelsius?.let { it / 10f },
    currentSumUa = currentSumUa,
    currentSamples = currentSamples,
    reachedFull = reachedFull,
)
