package com.faizakram.batterystatus.core.quirks

import java.util.Locale

/**
 * Per-manufacturer battery quirks. OEMs differ in where they expose sysfs battery
 * data and how they bound `current_now`, so the app looks these up by
 * [android.os.Build.MANUFACTURER] instead of assuming stock Android behavior.
 *
 * Everything here is a *hint*: sysfs reads still degrade to `null` and current
 * readings are still validated at runtime by [CurrentNormalizer].
 */
data class DeviceQuirks(
    val manufacturer: String,
    /** sysfs directories to probe, in priority order. */
    val sysfsSearchDirs: List<String>,
    /** `current_now` values with a larger magnitude are treated as garbage. */
    val maxPlausibleCurrentUa: Int,
) {
    companion object {
        /** 10 A: no phone battery legitimately reports more than this. */
        const val DEFAULT_MAX_PLAUSIBLE_CURRENT_UA = 10_000_000

        const val BATTERY_DIR = "/sys/class/power_supply/battery"

        private const val BMS_DIR = "/sys/class/power_supply/bms"
        private const val MAXFG_DIR = "/sys/class/power_supply/maxfg"

        val DEFAULT = DeviceQuirks(
            manufacturer = "",
            sysfsSearchDirs = listOf(BATTERY_DIR),
            maxPlausibleCurrentUa = DEFAULT_MAX_PLAUSIBLE_CURRENT_UA,
        )

        /** Resolves quirks for a `Build.MANUFACTURER` string (case-insensitive). */
        fun forManufacturer(manufacturer: String?): DeviceQuirks {
            val key = manufacturer?.trim()?.lowercase(Locale.US).orEmpty()
            return when (key) {
                "samsung" -> DeviceQuirks("samsung", listOf(BATTERY_DIR, BMS_DIR), DEFAULT_MAX_PLAUSIBLE_CURRENT_UA)
                "xiaomi" -> DeviceQuirks("xiaomi", listOf(BATTERY_DIR, BMS_DIR, MAXFG_DIR), DEFAULT_MAX_PLAUSIBLE_CURRENT_UA)
                "oneplus" -> DeviceQuirks("oneplus", listOf(BATTERY_DIR, BMS_DIR), DEFAULT_MAX_PLAUSIBLE_CURRENT_UA)
                "google" -> DeviceQuirks("google", listOf(BATTERY_DIR, BMS_DIR), DEFAULT_MAX_PLAUSIBLE_CURRENT_UA)
                else -> DEFAULT
            }
        }
    }
}
