package com.faizakram.batterystatus.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun `leading v prefix and whitespace are stripped`() {
        assertEquals("0.3.0", normalizeVersion("v0.3.0"))
        assertEquals("0.3.0", normalizeVersion("V0.3.0"))
        assertEquals("0.3.0", normalizeVersion("  0.3.0  "))
    }

    @Test
    fun `newer release is detected as an update`() {
        assertTrue(isNewerVersion("0.4.0", "0.3.0"))
        assertTrue(isNewerVersion("1.0.0", "0.9.9"))
        assertTrue(isNewerVersion("v0.4.0", "0.3.0"))
    }

    @Test
    fun `segments are compared numerically not lexically`() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
        assertFalse(isNewerVersion("0.9.0", "0.10.0"))
    }

    @Test
    fun `equal or older release is not an update`() {
        assertFalse(isNewerVersion("0.3.0", "0.3.0"))
        assertFalse(isNewerVersion("v0.3.0", "0.3.0"))
        assertFalse(isNewerVersion("0.2.9", "0.3.0"))
        assertFalse(isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun `missing trailing segments are treated as zero`() {
        assertTrue(isNewerVersion("1.0.1", "1.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
    }

    @Test
    fun `pre-release suffix does not outrank the same release`() {
        assertFalse(isNewerVersion("0.3.0-beta.1", "0.3.0"))
        assertTrue(isNewerVersion("0.3.1-beta.1", "0.3.0"))
    }
}
