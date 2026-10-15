package com.faizakram.batterystatus.di

import android.content.Context
import androidx.room.Room
import com.faizakram.batterystatus.data.local.BatteryDatabase
import com.faizakram.batterystatus.data.local.MIGRATION_3_4
import com.faizakram.batterystatus.data.local.dao.BatteryEstimatesDao
import com.faizakram.batterystatus.data.local.dao.BatterySnapshotDao
import com.faizakram.batterystatus.data.local.dao.ChargeSessionDao
import com.faizakram.batterystatus.data.local.dao.HourlySnapshotDao
import com.faizakram.batterystatus.data.local.dao.OpenChargeSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BatteryDatabase =
        Room.databaseBuilder(context, BatteryDatabase::class.java, BatteryDatabase.NAME)
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSnapshotDao(database: BatteryDatabase): BatterySnapshotDao =
        database.batterySnapshotDao()

    @Provides
    fun provideSessionDao(database: BatteryDatabase): ChargeSessionDao =
        database.chargeSessionDao()

    @Provides
    fun provideEstimatesDao(database: BatteryDatabase): BatteryEstimatesDao =
        database.batteryEstimatesDao()

    @Provides
    fun provideHourlySnapshotDao(database: BatteryDatabase): HourlySnapshotDao =
        database.hourlySnapshotDao()

    @Provides
    fun provideOpenSessionDao(database: BatteryDatabase): OpenChargeSessionDao =
        database.openChargeSessionDao()
}
