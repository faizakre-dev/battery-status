package com.faizakram.batterystatus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the single-row [com.faizakram.batterystatus.data.local.entity.OpenChargeSessionEntity]
 * table so an in-flight charge session survives process death.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `open_charge_session` (
                `id` INTEGER NOT NULL,
                `startMillis` INTEGER NOT NULL,
                `lastMillis` INTEGER NOT NULL,
                `percentFrom` INTEGER,
                `startChargeCounterUah` INTEGER,
                `lastChargeCounterUah` INTEGER,
                `maxPercent` INTEGER,
                `maxTemperatureDeciCelsius` INTEGER,
                `currentSumUa` INTEGER NOT NULL,
                `currentSamples` INTEGER NOT NULL,
                `reachedFull` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
