package com.faizakram.batterystatus.data.local

import androidx.room.TypeConverter
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType

class BatteryConverters {

    @TypeConverter
    fun batteryStatusToString(value: BatteryStatus): String = value.name

    @TypeConverter
    fun stringToBatteryStatus(value: String): BatteryStatus = BatteryStatus.valueOf(value)

    @TypeConverter
    fun batteryHealthToString(value: BatteryHealth): String = value.name

    @TypeConverter
    fun stringToBatteryHealth(value: String): BatteryHealth = BatteryHealth.valueOf(value)

    @TypeConverter
    fun plugTypeToString(value: PlugType): String = value.name

    @TypeConverter
    fun stringToPlugType(value: String): PlugType = PlugType.valueOf(value)
}
