package com.faizakram.batterystatus.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faizakram.batterystatus.data.repo.BatteryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: BatteryRepository,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryRange.DAY)
    val selectedRange: StateFlow<HistoryRange> = range.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val snapshots = range.flatMapLatest { window ->
        repository.observeSnapshotsSince(window.sinceMillis(System.currentTimeMillis()))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessions = range.flatMapLatest { window ->
        val since = window.sinceMillis(System.currentTimeMillis())
        repository.observeSessions().map { list -> list.filter { it.startMillis >= since } }
    }

    val uiState: StateFlow<HistoryUiState> = combine(snapshots, sessions, range) { snaps, sess, window ->
        HistoryUiState.from(window, snaps, sess)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState.from(HistoryRange.DAY, emptyList(), emptyList()),
    )

    fun selectRange(newRange: HistoryRange) {
        range.value = newRange
    }
}
