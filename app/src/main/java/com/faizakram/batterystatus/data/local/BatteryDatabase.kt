package com.faizakram.batterystatus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.faizakram.batterystatus.data.local.dao.BatteryEstimatesDao
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
import com.faizakram.batterystatus.data.local.dao.HourlySnapshotDao
import com.faizakram.batterystatus.data.local.dao.OpenChargeSessionDao
import com.faizakram.batterystatus.data.local.entity.BatteryEstimatesEntity
import com.faizakram.batterystatus.data.local.entity.BatterySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.ChargeSessionEntity
import com.faizakram.batterystatus.data.local.entity.HourlySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.OpenChargeSessionEntity

@Database(
    entities = [
        BatterySnapshotEntity::class,
        ChargeSessionEntity::class,
        BatteryEstimatesEntity::class,
        HourlySnapshotEntity::class,
        OpenChargeSessionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(BatteryConverters::class)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batterySnapshotDao(): BatterySnapshotDao
    abstract fun chargeSessionDao(): ChargeSessionDao
    abstract fun batteryEstimatesDao(): BatteryEstimatesDao
    abstract fun hourlySnapshotDao(): HourlySnapshotDao
    abstract fun openChargeSessionDao(): OpenChargeSessionDao

    companion object {
        const val NAME = "battery-status.db"
    }
}
