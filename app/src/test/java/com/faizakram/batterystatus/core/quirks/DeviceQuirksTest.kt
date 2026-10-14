package com.faizakram.batterystatus.core.quirks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceQuirksTest {

    @Test
    fun `samsung probes battery and bms`() {
        val quirks = DeviceQuirks.forManufacturer("samsung")
        assertEquals("samsung", quirks.manufacturer)
        assertEquals(listOf(DeviceQuirks.BATTERY_DIR, "/sys/class/power_supply/bms"), quirks.sysfsSearchDirs)
    }

    @Test
    fun `xiaomi also probes the maxfg node`() {
        val quirks = DeviceQuirks.forManufacturer("Xiaomi")
        assertTrue(quirks.sysfsSearchDirs.contains("/sys/class/power_supply/maxfg"))
    }

    @Test
    fun `matching is case insensitive and trims whitespace`() {
        assertEquals("oneplus", DeviceQuirks.forManufacturer(" OnePlus ").manufacturer)
        assertEquals("google", DeviceQuirks.forManufacturer("GOOGLE").manufacturer)
    }

    @Test
    fun `unknown manufacturers fall back to the default`() {
        assertEquals(DeviceQuirks.DEFAULT, DeviceQuirks.forManufacturer("SomeOem"))
        assertEquals(DeviceQuirks.DEFAULT, DeviceQuirks.forManufacturer(null))
        assertEquals(DeviceQuirks.DEFAULT, DeviceQuirks.forManufacturer(""))
    }
}
