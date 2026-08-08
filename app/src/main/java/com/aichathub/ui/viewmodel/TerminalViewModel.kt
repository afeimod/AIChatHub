package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.data.local.TerminalLogManager
import com.aichathub.domain.model.LogLevel
import com.aichathub.domain.model.TerminalLog
import com.aichathub.domain.usecase.ClearTerminalLogsUseCase
import com.aichathub.domain.usecase.GetTerminalLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val logs: List<TerminalLog> = emptyList(),
    val liveLogs: List<TerminalLog> = emptyList(),
    val autoScroll: Boolean = true,
    val filterLevel: LogLevel? = null,
    val filterText: String = "",
    val showTimestamp: Boolean = true,
    val isPaused: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val getTerminalLogsUseCase: GetTerminalLogsUseCase,
    private val clearTerminalLogsUseCase: ClearTerminalLogsUseCase,
    private val logManager: TerminalLogManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        // 持久化日志
        viewModelScope.launch {
            getTerminalLogsUseCase().collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
        // 实时日志
        viewModelScope.launch {
            logManager.liveLogs.collect { entry ->
                if (!_uiState.value.isPaused) {
                    _uiState.update {
                        val combined = (it.liveLogs + entry).takeLast(500)
                        it.copy(liveLogs = combined)
                    }
                }
            }
        }
    }

    fun toggleAutoScroll() {
        _uiState.update { it.copy(autoScroll = !it.autoScroll) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun setFilterLevel(level: LogLevel?) {
        _uiState.update { it.copy(filterLevel = level) }
    }

    fun setFilterText(text: String) {
        _uiState.update { it.copy(filterText = text) }
    }

    fun toggleTimestamp() {
        _uiState.update { it.copy(showTimestamp = !it.showTimestamp) }
    }

    fun clearLogs() {
        viewModelScope.launch {
            clearTerminalLogsUseCase()
            logManager.clearMemory()
            _uiState.update { it.copy(liveLogs = emptyList()) }
        }
    }

    fun manualLog(level: LogLevel, tag: String, message: String) {
        viewModelScope.launch {
            logManager.log(level, tag, message)
        }
    }

    /** 合并持久化 + 实时，按过滤器筛选 */
    fun filteredLogs(): List<TerminalLog> {
        val state = _uiState.value
        val merged = (state.logs + state.liveLogs).distinctBy { it.id }.sortedBy { it.timestamp }
        return merged.filter { log ->
            (state.filterLevel == null || log.level == state.filterLevel) &&
            (state.filterText.isBlank() ||
             log.message.contains(state.filterText, ignoreCase = true) ||
             log.tag.contains(state.filterText, ignoreCase = true))
        }
    }
}
