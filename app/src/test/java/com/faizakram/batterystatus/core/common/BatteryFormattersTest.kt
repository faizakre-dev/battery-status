package com.faizakram.batterystatus.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryFormattersTest {

    @Test
    fun `formats voltage in volts with two decimals`() {
        assertEquals("4.05 V", BatteryFormatters.voltage(4050))
        assertEquals("0.00 V", BatteryFormatters.voltage(0))
        assertNull(BatteryFormatters.voltage(null))
    }

    @Test
    fun `formats current in milliamps rounded to whole numbers`() {
        assertEquals("-412 mA", BatteryFormatters.currentMilliamps(-412_000))
        assertEquals("1500 mA", BatteryFormatters.currentMilliamps(1_500_000))
        assertNull(BatteryFormatters.currentMilliamps(null))
    }

    @Test
    fun `formats temperature in celsius with one decimal`() {
        assertEquals("31.4 °C", BatteryFormatters.temperature(31.4f))
        assertEquals("-2.0 °C", BatteryFormatters.temperature(-2f))
        assertNull(BatteryFormatters.temperature(null))
    }

    @Test
    fun `formats level percent and falls back for null`() {
        assertEquals("72%", BatteryFormatters.levelPercent(72))
        assertEquals("--", BatteryFormatters.levelPercent(null))
    }
}
