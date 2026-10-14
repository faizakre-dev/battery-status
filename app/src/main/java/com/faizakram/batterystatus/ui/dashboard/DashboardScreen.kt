package com.faizakram.batterystatus.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizakram.batterystatus.R
import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason
import com.faizakram.batterystatus.core.model.PlugType
import com.faizakram.batterystatus.ui.theme.BatteryStatusTheme
import com.faizakram.batterystatus.ui.theme.StatusPalette
import com.faizakram.batterystatus.ui.theme.statusPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState, modifier)
}

@Composable
private fun DashboardContent(state: DashboardUiState?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state == null) {
            Text(
                text = stringResource(R.string.dashboard_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        BatteryLevelRing(state)
        HealthBadge(state)
        EstimatesRow(state)
        LiveStats(state)

        state.technology?.let { technology ->
            Text(
                text = stringResource(R.string.dashboard_technology, technology),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = stringResource(R.string.dashboard_updated, formatTime(state.updatedAtMillis)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryLevelRing(state: DashboardUiState) {
    val fraction = ((state.levelPercent ?: 0) / 100f).coerceIn(0f, 1f)
    val ringColor = ringColor(state, statusPalette())
    val statusText = stringResource(statusLabelRes(state.status, state.plugged))
    val levelText = state.levelPercent?.let { "$it%" }
    val description = if (levelText != null) {
        stringResource(R.string.a11y_level_ring, levelText, statusText)
    } else {
        stringResource(R.string.a11y_level_ring_unknown, statusText)
    }

    Box(
        modifier = Modifier
            .size(220.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 18.dp,
            strokeCap = StrokeCap.Round,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics {},
        ) {
            Text(
                text = levelText ?: "--",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HealthBadge(state: DashboardUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val color = healthColor(state.healthLevel, statusPalette())
        Surface(
            color = color.copy(alpha = 0.15f),
            contentColor = color,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = stringResource(healthLabelRes(state.healthLevel)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Text(
            text = state.healthReasons
                .map { stringResource(reasonRes(it)) }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EstimatesRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = stringResource(R.string.dashboard_stat_health),
            value = state.healthPercent?.let { "$it%" },
            modifier = Modifier.weight(1f),
            estimated = state.healthPercentEstimated,
        )
        StatCard(
            label = stringResource(R.string.dashboard_stat_cycles),
            value = BatteryFormatters.cycles(state.estimatedCycles),
            modifier = Modifier.weight(1f),
            estimated = !state.cyclesFromSystem,
        )
    }
}

@Composable
private fun LiveStats(state: DashboardUiState) {
    val timeValue = state.timeToFullText ?: state.timeRemainingText
    val timeLabel = stringResource(
        if (state.timeToFullText != null) R.string.dashboard_stat_time_to_full
        else R.string.dashboard_stat_time_left,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.dashboard_stat_temperature),
                state.temperatureText,
                Modifier.weight(1f),
            )
            StatCard(
                stringResource(R.string.dashboard_stat_voltage),
                state.voltageText,
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                stringResource(R.string.dashboard_stat_current),
                state.currentText,
                Modifier.weight(1f),
            )
            StatCard(timeLabel, timeValue, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    estimated: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (estimated) stringResource(R.string.stat_label_estimated, label) else label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when {
                    value == null -> stringResource(R.string.stat_placeholder)
                    estimated -> stringResource(R.string.stat_approx, value)
                    else -> value
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

private fun ringColor(state: DashboardUiState, palette: StatusPalette): Color = when {
    state.status == BatteryStatus.CHARGING || state.status == BatteryStatus.FULL -> palette.good
    state.levelPercent == null -> palette.neutral
    state.levelPercent <= 15 -> palette.danger
    state.levelPercent <= 30 -> palette.warning
    else -> palette.good
}

private fun healthColor(level: HealthLevel, palette: StatusPalette): Color = when (level) {
    HealthLevel.GOOD -> palette.good
    HealthLevel.ATTENTION -> palette.warning
    HealthLevel.POOR -> palette.danger
    HealthLevel.UNKNOWN -> palette.neutral
}

@StringRes
private fun healthLabelRes(level: HealthLevel): Int = when (level) {
    HealthLevel.GOOD -> R.string.health_good
    HealthLevel.ATTENTION -> R.string.health_attention
    HealthLevel.POOR -> R.string.health_poor
    HealthLevel.UNKNOWN -> R.string.health_unknown
}

@StringRes
private fun reasonRes(reason: HealthReason): Int = when (reason) {
    HealthReason.HEALTHY -> R.string.reason_healthy
    HealthReason.OVERHEAT -> R.string.reason_overheat
    HealthReason.COLD -> R.string.reason_cold
    HealthReason.DEAD -> R.string.reason_dead
    HealthReason.OVER_VOLTAGE -> R.string.reason_over_voltage
    HealthReason.FAILURE -> R.string.reason_failure
    HealthReason.UNKNOWN -> R.string.reason_unknown
    HealthReason.TEMPERATURE_HIGH -> R.string.reason_temperature_high
    HealthReason.TEMPERATURE_LOW -> R.string.reason_temperature_low
}

@StringRes
private fun statusLabelRes(status: BatteryStatus, plugged: PlugType): Int = when (status) {
    BatteryStatus.CHARGING -> R.string.status_charging
    BatteryStatus.FULL -> R.string.status_full
    BatteryStatus.DISCHARGING -> R.string.status_discharging
    BatteryStatus.NOT_CHARGING ->
        if (plugged != PlugType.NONE) R.string.status_plugged_in else R.string.status_not_charging
    BatteryStatus.UNKNOWN -> R.string.status_unknown
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    BatteryStatusTheme {
        DashboardContent(
            DashboardUiState(
                levelPercent = 72,
                status = BatteryStatus.DISCHARGING,
                plugged = PlugType.NONE,
                healthLevel = HealthLevel.GOOD,
                healthReasons = listOf(HealthReason.HEALTHY),
                temperatureText = "31.4 °C",
                voltageText = "4.05 V",
                currentText = "-412 mA",
                technology = "Li-ion",
                updatedAtMillis = 0L,
                estimatedCycles = 128.0,
                cyclesFromSystem = false,
                healthPercent = 92,
                healthPercentEstimated = true,
                timeToFullText = null,
                timeRemainingText = "5h 12m",
            ),
        )
    }
}
