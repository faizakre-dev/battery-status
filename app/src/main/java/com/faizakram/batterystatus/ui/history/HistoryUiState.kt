package com.faizakram.batterystatus.ui.history

import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.model.BatterySnapshot
import com.faizakram.batterystatus.core.model.ChargeSession

/** The selectable look-back windows for the history screen. */
enum class HistoryRange(val label: String, val durationMillis: Long) {
    DAY("24h", 24L * 60 * 60 * 1000),
    WEEK("7d", 7L * 24 * 60 * 60 * 1000),
}

/** Presentation-ready charge session row. */
data class SessionUi(
    val id: Long,
    val startMillis: Long,
    val endMillis: Long,
    val percentFrom: Int?,
    val percentTo: Int?,
    val mahAdded: Int?,
    val maxTemperatureText: String?,
    val durationText: String?,
    val isFull: Boolean,
)

data class HistoryUiState(
    val range: HistoryRange,
    val levelSeries: ChartSeries,
    val temperatureSeries: ChartSeries,
    val sessions: List<SessionUi>,
) {
    val hasAnyData: Boolean
        get() = !levelSeries.isEmpty || !temperatureSeries.isEmpty || sessions.isNotEmpty()

    companion object {
        fun from(
            range: HistoryRange,
            snapshots: List<BatterySnapshot>,
            sessions: List<ChargeSession>,
        ): HistoryUiState = HistoryUiState(
            range = range,
            levelSeries = ChartModels.levelSeries(snapshots),
            temperatureSeries = ChartModels.temperatureSeries(snapshots),
            sessions = sessions
                .sortedByDescending { it.startMillis }
                .map { it.toSessionUi() },
        )
    }
}

fun ChargeSession.toSessionUi(): SessionUi = SessionUi(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    percentFrom = percentFrom,
    percentTo = percentTo,
    mahAdded = mahAdded,
    maxTemperatureText = BatteryFormatters.temperature(maxTemperatureCelsius),
    durationText = BatteryFormatters.duration((endMillis - startMillis).takeIf { it > 0 }),
    isFull = isFull,
)
