package com.faizakram.batterystatus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
import com.faizakram.batterystatus.data.local.entity.BatterySnapshotEntity
import com.faizakram.batterystatus.data.local.entity.ChargeSessionEntity

@Database(
    entities = [
        BatterySnapshotEntity::class,
        ChargeSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(BatteryConverters::class)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batterySnapshotDao(): BatterySnapshotDao
    abstract fun chargeSessionDao(): ChargeSessionDao

    companion object {
        const val NAME = "battery-status.db"
    }
}
