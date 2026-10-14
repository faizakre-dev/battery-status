package com.faizakram.batterystatus.core.sysfs

import java.io.File
import java.io.IOException

/**
 * Battery design/capacity values that are normally only readable from sysfs.
 * Every field is best-effort; `null` means "not exposed on this device".
 */
data class SysfsBatteryData(
    val cycleCount: Int?,
    val chargeFullUah: Int?,
    val chargeFullDesignUah: Int?,
) {
    companion object {
        val NONE = SysfsBatteryData(cycleCount = null, chargeFullUah = null, chargeFullDesignUah = null)
    }
}

interface SysfsBatteryReader {
    /** Reads the battery's sysfs entries once; never throws. */
    fun read(): SysfsBatteryData
}

/**
 * Best-effort reader for the `/sys/class/power_supply` battery nodes. SELinux blocks
 * app reads of these files, so every read degrades to `null` on failure rather
 * than crashing. Different OEMs expose the entries under different directories
 * (battery/bms/maxfg), so [read] probes each directory in order and takes the
 * first readable value per field.
 */
class BestEffortSysfsReader(
    private val searchDirs: List<File> = DEFAULT_DIRS,
) : SysfsBatteryReader {

    constructor(batteryDir: File) : this(listOf(batteryDir))

    override fun read(): SysfsBatteryData = SysfsBatteryData(
        cycleCount = readPositiveInt(CYCLE_COUNT),
        chargeFullUah = readPositiveInt(CHARGE_FULL),
        chargeFullDesignUah = readPositiveInt(CHARGE_FULL_DESIGN),
    )

    private fun readPositiveInt(name: String): Int? {
        for (dir in searchDirs) {
            val value = readText(dir, name)
                ?.trim()
                ?.toLongOrNull()
                ?.takeIf { it > 0 && it <= Int.MAX_VALUE }
                ?.toInt()
            if (value != null) return value
        }
        return null
    }

    private fun readText(dir: File, name: String): String? = try {
        val file = File(dir, name)
        if (file.isFile && file.canRead()) file.readText() else null
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    companion object {
        const val DEFAULT_PATH = "/sys/class/power_supply/battery"
        const val CYCLE_COUNT = "cycle_count"
        const val CHARGE_FULL = "charge_full"
        const val CHARGE_FULL_DESIGN = "charge_full_design"

        val DEFAULT_DIRS: List<File> = listOf(File(DEFAULT_PATH))
    }
}
