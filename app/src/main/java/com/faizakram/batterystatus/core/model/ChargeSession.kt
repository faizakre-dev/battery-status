package com.faizakram.batterystatus.core.model

/**
 * A charging/discharging session. [id] is 0 until persisted by the repository.
 */
data class ChargeSession(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val percentFrom: Int?,
    val percentTo: Int?,
    val mahAdded: Int?,
    val maxTemperatureCelsius: Float?,
    val avgCurrentUa: Int?,
    val isFull: Boolean,
)
