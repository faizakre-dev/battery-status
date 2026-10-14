package com.faizakram.batterystatus.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faizakram.batterystatus.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onSelectRange = viewModel::selectRange,
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onSelectRange: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                RangeSelector(state.range, onSelectRange, Modifier.padding(top = 12.dp))
            }
        }

        item {
            ChartCard(
                title = stringResource(R.string.history_chart_level),
                series = state.levelSeries,
                lineColor = MaterialTheme.colorScheme.primary,
                valueLabel = { "$it%" },
                descriptionRes = R.string.a11y_chart_level,
            )
        }

        item {
            ChartCard(
                title = stringResource(R.string.history_chart_temperature),
                series = state.temperatureSeries,
                lineColor = MaterialTheme.colorScheme.tertiary,
                valueLabel = { String.format(Locale.US, "%.1f °C", it) },
                descriptionRes = R.string.a11y_chart_temperature,
            )
        }

        item {
            Text(
                text = stringResource(R.string.history_sessions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (state.sessions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.history_no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.sessions, key = { it.id }) { session ->
                SessionRow(session)
            }
        }

        item { Column(modifier = Modifier.height(8.dp)) {} }
    }
}

@Composable
private fun RangeSelector(
    selected: HistoryRange,
    onSelectRange: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelectRange(range) },
                label = { Text(stringResource(range.labelRes)) },
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    series: ChartSeries,
    lineColor: Color,
    valueLabel: (Float) -> String,
    @StringRes descriptionRes: Int,
) {
    val min = series.minValue
    val max = series.maxValue
    val description = if (min != null && max != null) {
        stringResource(descriptionRes, valueLabel(min), valueLabel(max))
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (min != null && max != null) {
                    Text(
                        text = stringResource(R.string.history_range_summary, valueLabel(min), valueLabel(max)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LineChart(
                series = series,
                lineColor = lineColor,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SessionRow(session: SessionUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = percentSummary(session),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (session.isFull) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.history_session_full),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = formatDateTime(session.startMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = detailLine(session),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun percentSummary(session: SessionUi): String {
    val from = session.percentFrom?.let { "$it%" } ?: "--"
    val to = session.percentTo?.let { "$it%" } ?: "--"
    return stringResource(R.string.history_session_percent, from, to)
}

@Composable
private fun detailLine(session: SessionUi): String {
    val parts = buildList {
        session.mahAdded?.let { add(stringResource(R.string.history_session_mah, it)) }
        session.durationText?.let { add(it) }
        session.maxTemperatureText?.let { add(stringResource(R.string.history_session_max_temp, it)) }
    }
    return if (parts.isEmpty()) {
        stringResource(R.string.history_session_none)
    } else {
        parts.joinToString(" · ")
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()).format(Date(millis))
