package com.faizakram.batterystatus.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesignCapacityResolverTest {

    @Test
    fun `prefers the sysfs design capacity`() {
        assertEquals(
            4_000_000,
            DesignCapacityResolver.resolve(sysfsDesignUah = 4_000_000, currentDesignUah = 3_000_000, lifetimePeakUah = 2_000_000),
        )
    }

    @Test
    fun `keeps the stored design capacity when sysfs is unavailable`() {
        assertEquals(
            3_000_000,
            DesignCapacityResolver.resolve(sysfsDesignUah = null, currentDesignUah = 3_000_000, lifetimePeakUah = 2_000_000),
        )
    }

    @Test
    fun `falls back to 1_1x the lifetime peak`() {
        assertEquals(
            2_200_000,
            DesignCapacityResolver.resolve(sysfsDesignUah = null, currentDesignUah = null, lifetimePeakUah = 2_000_000),
        )
    }

    @Test
    fun `returns null when nothing is known`() {
        assertNull(DesignCapacityResolver.resolve(null, null, null))
        assertNull(DesignCapacityResolver.resolve(0, 0, 0))
    }
}
