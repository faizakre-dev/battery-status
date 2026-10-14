package com.faizakram.batterystatus.core.quirks

import com.faizakram.batterystatus.core.model.BatteryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentNormalizerTest {

    @Test
    fun `keeps positive current while charging`() {
        assertEquals(1_500_000, CurrentNormalizer.normalize(1_500_000, BatteryStatus.CHARGING))
    }

    @Test
    fun `flips negative current while charging to positive`() {
        assertEquals(1_500_000, CurrentNormalizer.normalize(-1_500_000, BatteryStatus.CHARGING))
    }

    @Test
    fun `keeps negative current while discharging`() {
        assertEquals(-412_000, CurrentNormalizer.normalize(-412_000, BatteryStatus.DISCHARGING))
    }

    @Test
    fun `flips positive current while discharging to negative`() {
        assertEquals(-412_000, CurrentNormalizer.normalize(412_000, BatteryStatus.DISCHARGING))
    }

    @Test
    fun `treats full as charging`() {
        assertEquals(250_000, CurrentNormalizer.normalize(-250_000, BatteryStatus.FULL))
    }

    @Test
    fun `leaves sign untouched when direction is ambiguous`() {
        assertEquals(300_000, CurrentNormalizer.normalize(300_000, BatteryStatus.NOT_CHARGING))
        assertEquals(-300_000, CurrentNormalizer.normalize(-300_000, BatteryStatus.UNKNOWN))
    }

    @Test
    fun `returns null for missing value`() {
        assertNull(CurrentNormalizer.normalize(null, BatteryStatus.CHARGING))
    }

    @Test
    fun `passes through zero`() {
        assertEquals(0, CurrentNormalizer.normalize(0, BatteryStatus.DISCHARGING))
    }

    @Test
    fun `rejects implausibly large magnitudes`() {
        assertNull(CurrentNormalizer.normalize(99_000_000, BatteryStatus.CHARGING))
        assertNull(CurrentNormalizer.normalize(-99_000_000, BatteryStatus.DISCHARGING))
    }

    @Test
    fun `honours a custom plausibility bound`() {
        assertNull(CurrentNormalizer.normalize(2_000_000, BatteryStatus.CHARGING, maxPlausibleMicroAmps = 1_000_000))
    }
}
