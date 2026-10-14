package com.faizakram.batterystatus.di

import android.content.Context
import com.faizakram.batterystatus.core.battery.AndroidBatteryInfoSource
import com.faizakram.batterystatus.core.battery.BatteryInfoSource
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
}
