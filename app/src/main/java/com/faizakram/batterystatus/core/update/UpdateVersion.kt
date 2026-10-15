package com.faizakram.batterystatus.core.update

/**
 * Strips formatting noise from a release tag/version string so only the
 * numeric version remains (e.g. `"v0.3.0"` → `"0.3.0"`).
 */
fun normalizeVersion(raw: String): String =
    raw.trim().removePrefix("v").removePrefix("V")

/**
 * Compares two semantic-ish version strings numerically segment by segment
 * (`"0.10.0"` > `"0.9.0"`), ignoring a leading `v`, missing trailing segments
 * (treated as 0) and any `-pre` / `+build` suffix.
 *
 * Returns `true` only when [latest] is strictly newer than [current], so a
 * downgraded or equal release is never offered as an update.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = versionSegments(latest)
    val currentParts = versionSegments(current)
    val length = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until length) {
        val newer = latestParts.getOrElse(index) { 0 }
        val older = currentParts.getOrElse(index) { 0 }
        if (newer != older) return newer > older
    }
    return false
}

private fun versionSegments(version: String): List<Int> = normalizeVersion(version)
    .substringBefore('-')
    .substringBefore('+')
    .split('.')
    .map { segment -> segment.toIntOrNull() ?: 0 }
