package com.faizakram.batterystatus.core.battery

import android.os.BatteryManager
import com.faizakram.batterystatus.core.model.BatteryHealth
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMappersTest {

    @Test
    fun `maps known battery statuses`() {
        assertEquals(BatteryStatus.CHARGING, batteryStatusFrom(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals(BatteryStatus.DISCHARGING, batteryStatusFrom(BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals(BatteryStatus.FULL, batteryStatusFrom(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals(BatteryStatus.NOT_CHARGING, batteryStatusFrom(BatteryManager.BATTERY_STATUS_NOT_CHARGING))
    }

    @Test
    fun `unknown battery status falls back to UNKNOWN`() {
        assertEquals(BatteryStatus.UNKNOWN, batteryStatusFrom(999))
    }

    @Test
    fun `maps known health values`() {
        assertEquals(BatteryHealth.GOOD, batteryHealthFrom(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals(BatteryHealth.OVERHEAT, batteryHealthFrom(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertEquals(BatteryHealth.DEAD, batteryHealthFrom(BatteryManager.BATTERY_HEALTH_DEAD))
        assertEquals(BatteryHealth.OVER_VOLTAGE, batteryHealthFrom(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE))
        assertEquals(BatteryHealth.COLD, batteryHealthFrom(BatteryManager.BATTERY_HEALTH_COLD))
    }

    @Test
    fun `maps plug types`() {
        assertEquals(PlugType.NONE, plugTypeFrom(0))
        assertEquals(PlugType.AC, plugTypeFrom(BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(PlugType.USB, plugTypeFrom(BatteryManager.BATTERY_PLUGGED_USB))
        assertEquals(PlugType.WIRELESS, plugTypeFrom(BatteryManager.BATTERY_PLUGGED_WIRELESS))
        assertEquals(PlugType.DOCK, plugTypeFrom(BatteryManager.BATTERY_PLUGGED_DOCK))
    }

    @Test
    fun `converts tenths of a degree to celsius`() {
        assertEquals(25.5f, temperatureCelsius(255)!!, 0.001f)
        assertEquals(-3.0f, temperatureCelsius(-30)!!, 0.001f)
    }

    @Test
    fun `missing temperature returns null`() {
        assertNull(temperatureCelsius(Int.MIN_VALUE))
    }
}
