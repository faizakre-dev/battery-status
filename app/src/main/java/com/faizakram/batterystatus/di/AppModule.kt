package com.faizakram.batterystatus.di

import android.content.Context
import android.os.Build
import com.faizakram.batterystatus.core.battery.AndroidBatteryInfoSource
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.quirks.DeviceQuirks
import com.faizakram.batterystatus.core.sysfs.BestEffortSysfsReader
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryReader
import com.faizakram.batterystatus.data.repo.BatteryRepository
import com.faizakram.batterystatus.data.repo.BatteryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
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
    fun provideDeviceQuirks(): DeviceQuirks = DeviceQuirks.forManufacturer(Build.MANUFACTURER)

    @Provides
    @Singleton
    fun provideSysfsBatteryReader(quirks: DeviceQuirks): SysfsBatteryReader =
        BestEffortSysfsReader(quirks.sysfsSearchDirs.map(::File))

    @Provides
    @Singleton
    fun provideBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository = impl
}
