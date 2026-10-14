package com.faizakram.batterystatus.core.sysfs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BestEffortSysfsReaderTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `reads all three values when present`() {
        val dir = temp.newFolder("battery")
        File(dir, "cycle_count").writeText("312\n")
        File(dir, "charge_full").writeText("3850000\n")
        File(dir, "charge_full_design").writeText("4000000\n")

        val data = BestEffortSysfsReader(dir).read()

        assertEquals(312, data.cycleCount)
        assertEquals(3_850_000, data.chargeFullUah)
        assertEquals(4_000_000, data.chargeFullDesignUah)
    }

    @Test
    fun `ignores missing and malformed files`() {
        val dir = temp.newFolder("battery")
        File(dir, "charge_full").writeText("not-a-number\n")
        File(dir, "charge_full_design").writeText("-5\n")

        val data = BestEffortSysfsReader(dir).read()

        assertNull(data.cycleCount)
        assertNull(data.chargeFullUah)
        assertNull(data.chargeFullDesignUah)
    }

    @Test
    fun `falls back to later directories per field`() {
        val battery = temp.newFolder("battery")
        val bms = temp.newFolder("bms")
        File(battery, "cycle_count").writeText("-1\n")
        File(bms, "cycle_count").writeText("204\n")
        File(bms, "charge_full").writeText("3800000\n")

        val data = BestEffortSysfsReader(listOf(battery, bms)).read()

        assertEquals(204, data.cycleCount)
        assertEquals(3_800_000, data.chargeFullUah)
        assertNull(data.chargeFullDesignUah)
    }

    @Test
    fun `returns all nulls when the directory does not exist`() {
        val dir = File(temp.root, "does-not-exist")

        val data = BestEffortSysfsReader(dir).read()

        assertEquals(SysfsBatteryData.NONE, data)
    }
}
