package com.aimp.player.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimp.player.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LyricsUiState {
    data object Idle : LyricsUiState()
    data object Loading : LyricsUiState()
    data class Loaded(val lines: List<LyricLine>, val synced: Boolean, val currentIndex: Int) : LyricsUiState()
    data class NotFound(val message: String) : LyricsUiState()
    data class Error(val message: String) : LyricsUiState()
}

class LyricsViewModel(
    private val repository: LyricsRepository = LyricsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    private val _autoFetch = MutableStateFlow(true)
    val autoFetch: StateFlow<Boolean> = _autoFetch.asStateFlow()

    private var loadedLines: List<LyricLine> = emptyList()
    private var synced: Boolean = false

    fun setAutoFetch(enabled: Boolean) { _autoFetch.value = enabled }

    fun load(track: Track) {
        viewModelScope.launch {
            _state.value = LyricsUiState.Loading
            when (val res = repository.fetchForTrack(track)) {
                is LyricsResult.Success -> {
                    loadedLines = res.lines
                    synced = res.synced
                    _state.value = LyricsUiState.Loaded(loadedLines, synced, 0)
                }
                is LyricsResult.NotFound -> _state.value = LyricsUiState.NotFound(res.message)
                is LyricsResult.Error -> _state.value = LyricsUiState.Error(res.throwable.message ?: "Failed to fetch lyrics")
            }
        }
    }

    fun updatePosition(posMs: Long) {
        val s = _state.value
        if (s is LyricsUiState.Loaded && s.synced && loadedLines.isNotEmpty()) {
            var idx = 0
            for (i in loadedLines.indices) {
                if (loadedLines[i].timeMs <= posMs) idx = i else break
            }
            if (idx != s.currentIndex) _state.value = s.copy(currentIndex = idx)
        }
    }
}
