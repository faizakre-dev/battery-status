package com.faizakram.batterystatus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType

@Entity(
    tableName = "battery_snapshots",
    indices = [Index(value = ["timestampMillis"])],
)
data class BatterySnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val levelPercent: Int?,
    val status: BatteryStatus,
    val health: BatteryHealth,
    val plugged: PlugType,
    val voltageMv: Int?,
    /** Temperature in tenths of a degree Celsius (e.g. 253 == 25.3 °C). */
    val temperatureDeciCelsius: Int?,
    val currentUa: Int?,
    val chargeCounterUah: Int?,
    val powerMw: Int?,
)

@Entity(
    tableName = "charge_sessions",
    indices = [Index(value = ["startMillis"])],
)
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val percentFrom: Int?,
    val percentTo: Int?,
    val mahAdded: Int?,
    /** Maximum session temperature in tenths of a degree Celsius. */
    val maxTemperatureDeciCelsius: Int?,
    val avgCurrentUa: Int?,
    val isFull: Boolean,
)
