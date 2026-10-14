package com.faizakram.batterystatus.di

import android.content.Context
import com.faizakram.batterystatus.core.battery.AndroidBatteryInfoSource
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.sysfs.BestEffortSysfsReader
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryReader
import com.faizakram.batterystatus.data.repo.BatteryRepository
import com.faizakram.batterystatus.data.repo.BatteryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBatteryInfoSource(
        @ApplicationContext context: Context,
    ): BatteryInfoSource = AndroidBatteryInfoSource(context)

    @Provides
    @Singleton
    fun provideSysfsBatteryReader(): SysfsBatteryReader = BestEffortSysfsReader()

    @Provides
    @Singleton
    fun provideBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository = impl
}
