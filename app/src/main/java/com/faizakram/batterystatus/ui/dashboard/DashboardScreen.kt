package com.faizakram.batterystatus.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizakram.batterystatus.core.model.BatterySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    DashboardContent(snapshot)
}

@Composable
private fun DashboardContent(snapshot: BatterySnapshot?) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Raw battery data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()

            if (snapshot == null) {
                Text("Waiting for battery broadcast…")
                return@Column
            }

            RawRow("Level", snapshot.levelPercent?.let { "$it%" })
            RawRow("Capacity (property)", snapshot.capacityPercent?.let { "$it%" })
            RawRow("Status", snapshot.status.name)
            RawRow("Health", snapshot.health.name)
            RawRow("Plugged", snapshot.plugged.name)
            RawRow("Scale", snapshot.scale.toString())
            RawRow("Voltage", snapshot.voltageMv?.let { "%.2f V".format(it / 1000f) })
            RawRow("Temperature", snapshot.temperatureCelsius?.let { "%.1f °C".format(it) })
            RawRow("Technology", snapshot.technology)
            RawRow("Charge counter", snapshot.chargeCounterUah?.let { "$it µAh" })
            RawRow("Current now", snapshot.currentNowUa?.let { "$it µA" })
            RawRow("Current average", snapshot.currentAvgUa?.let { "$it µA" })
            RawRow("Energy counter", snapshot.energyCounterUah?.let { "$it µAh" })
            RawRow("Timestamp", formatTimestamp(snapshot.timestampMillis))
        }
    }
}

@Composable
private fun RawRow(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
