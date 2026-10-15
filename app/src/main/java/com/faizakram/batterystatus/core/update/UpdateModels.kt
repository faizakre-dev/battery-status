package com.faizakram.batterystatus.core.update

/** Details of the latest GitHub release that carries an installable APK. */
data class ReleaseInfo(
    /** Raw release tag, e.g. `"v0.4.0"`. */
    val tagName: String,
    /** Normalized numeric version derived from [tagName], e.g. `"0.4.0"`. */
    val version: String,
    /** Human-readable release title. */
    val name: String,
    /** Release notes body (may be blank). */
    val notes: String,
    /** Direct download URL of the APK asset. */
    val apkUrl: String,
    /** ISO-8601 publication timestamp as reported by GitHub. */
    val publishedAt: String,
)

/** Failure categories surfaced to the UI as localized messages. */
enum class UpdateError { NETWORK, NOT_FOUND, PARSE, NO_APK, STORAGE }

/** Carrier for an [UpdateError] thrown by the update pipeline. */
class UpdateException(val error: UpdateError) : Exception(error.name)

/** Lifecycle of the "check → download → install" flow. */
sealed interface UpdateState {

    /** No check has run yet (fresh process start). */
    data object Idle : UpdateState

    /** A release lookup is in flight. */
    data object Checking : UpdateState

    /** The installed version is the latest release. */
    data object UpToDate : UpdateState

    /** A newer release exists and can be downloaded. */
    data class Available(val release: ReleaseInfo) : UpdateState

    /** The APK is being downloaded; `progressPercent` is null when unknown. */
    data class Downloading(val release: ReleaseInfo, val progressPercent: Int?) : UpdateState

    /** The APK is fully downloaded and waiting for the install prompt. */
    data class ReadyToInstall(val release: ReleaseInfo) : UpdateState

    /**
     * Something went wrong. [release] is non-null when a download failed,
     * allowing a retry without re-running the check.
     */
    data class Failed(val error: UpdateError, val release: ReleaseInfo? = null) : UpdateState
}
