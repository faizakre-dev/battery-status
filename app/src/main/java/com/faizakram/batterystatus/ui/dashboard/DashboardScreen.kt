package com.faizakram.batterystatus.ui.dashboard

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizakram.batterystatus.core.common.BatteryFormatters
import com.faizakram.batterystatus.core.model.BatteryStatus
import com.faizakram.batterystatus.core.model.HealthLevel
import com.faizakram.batterystatus.core.model.HealthReason
import com.faizakram.batterystatus.core.model.PlugType
import com.faizakram.batterystatus.ui.theme.BatteryStatusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GoodGreen = Color(0xFF2E7D32)
private val WarningAmber = Color(0xFFF9A825)
private val DangerRed = Color(0xFFC62828)
private val NeutralGray = Color(0xFF9E9E9E)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState)
}

@Composable
private fun DashboardContent(state: DashboardUiState?) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Battery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state == null) {
                Text(
                    text = "Reading battery status…",
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
                    text = "Technology: $technology",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Updated ${formatTime(state.updatedAtMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BatteryLevelRing(state: DashboardUiState) {
    val fraction = ((state.levelPercent ?: 0) / 100f).coerceIn(0f, 1f)
    val ringColor = ringColor(state)

    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 18.dp,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.levelPercent?.let { "$it%" } ?: "--",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = statusLabel(state.status, state.plugged),
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
        val color = healthColor(state.healthLevel)
        Surface(
            color = color.copy(alpha = 0.15f),
            contentColor = color,
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = healthLabel(state.healthLevel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Text(
            text = state.healthReasons.joinToString(" · ", transform = ::reasonLabel),
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
            label = "Health",
            value = state.healthPercent?.let { "$it%" },
            modifier = Modifier.weight(1f),
            estimated = state.healthPercentEstimated,
        )
        StatCard(
            label = "Cycles",
            value = BatteryFormatters.cycles(state.estimatedCycles),
            modifier = Modifier.weight(1f),
            estimated = !state.cyclesFromSystem,
        )
    }
}

@Composable
private fun LiveStats(state: DashboardUiState) {
    val timeValue = state.timeToFullText ?: state.timeRemainingText
    val timeLabel = if (state.timeToFullText != null) "Time to full" else "Time left"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Temperature", state.temperatureText, Modifier.weight(1f))
            StatCard("Voltage", state.voltageText, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Current", state.currentText, Modifier.weight(1f))
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (estimated) "$label · est." else label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when {
                    value == null -> "—"
                    estimated -> "≈ $value"
                    else -> value
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

private fun ringColor(state: DashboardUiState): Color = when {
    state.status == BatteryStatus.CHARGING || state.status == BatteryStatus.FULL -> GoodGreen
    state.levelPercent == null -> NeutralGray
    state.levelPercent <= 15 -> DangerRed
    state.levelPercent <= 30 -> WarningAmber
    else -> GoodGreen
}

private fun healthColor(level: HealthLevel): Color = when (level) {
    HealthLevel.GOOD -> GoodGreen
    HealthLevel.ATTENTION -> WarningAmber
    HealthLevel.POOR -> DangerRed
    HealthLevel.UNKNOWN -> NeutralGray
}

private fun healthLabel(level: HealthLevel): String = when (level) {
    HealthLevel.GOOD -> "Good"
    HealthLevel.ATTENTION -> "Attention"
    HealthLevel.POOR -> "Poor"
    HealthLevel.UNKNOWN -> "Unknown"
}

private fun reasonLabel(reason: HealthReason): String = when (reason) {
    HealthReason.HEALTHY -> "Healthy"
    HealthReason.OVERHEAT -> "Overheating"
    HealthReason.COLD -> "Too cold"
    HealthReason.DEAD -> "Battery dead"
    HealthReason.OVER_VOLTAGE -> "Over voltage"
    HealthReason.FAILURE -> "Reported failure"
    HealthReason.UNKNOWN -> "Health unavailable"
    HealthReason.TEMPERATURE_HIGH -> "High temperature"
    HealthReason.TEMPERATURE_LOW -> "Low temperature"
}

private fun statusLabel(status: BatteryStatus, plugged: PlugType): String = when (status) {
    BatteryStatus.CHARGING -> "Charging"
    BatteryStatus.FULL -> "Fully charged"
    BatteryStatus.DISCHARGING -> "Discharging"
    BatteryStatus.NOT_CHARGING -> if (plugged != PlugType.NONE) "Plugged in" else "Not charging"
    BatteryStatus.UNKNOWN -> "Unknown"
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
