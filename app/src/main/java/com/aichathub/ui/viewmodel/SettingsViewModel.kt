package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.AppSettings
import com.aichathub.domain.model.ContextStrategy
import com.aichathub.domain.repository.SettingsRepository
import com.aichathub.domain.usecase.ClearAllSessionsUseCase
import com.aichathub.domain.usecase.GetSettingsUseCase
import com.aichathub.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showClearConfirmDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val clearAllSessionsUseCase: ClearAllSessionsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(isDarkMode = !s.isDarkMode))
        }
    }

    fun toggleStreamResponse() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(enableStreamResponse = !s.enableStreamResponse))
        }
    }

    fun toggleMultimodal() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(enableMultimodal = !s.enableMultimodal))
        }
    }

    fun toggleMarkdown() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(enableMarkdown = !s.enableMarkdown))
        }
    }

    fun toggleTerminalLog() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(enableTerminalLog = !s.enableTerminalLog))
        }
    }

    fun toggleTokenCounter() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(enableTokenCounter = !s.enableTokenCounter))
        }
    }

    fun toggleAutoTitle() {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(autoTitleFromFirstMessage = !s.autoTitleFromFirstMessage))
        }
    }

    fun updateDefaultPlatform(platform: AIPlatform) {
        viewModelScope.launch { settingsRepository.setDefaultPlatform(platform) }
    }

    fun updateDefaultTemperature(value: Float) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(defaultTemperature = value))
        }
    }

    fun updateDefaultMaxTokens(value: Int) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(defaultMaxTokens = value))
        }
    }

    fun updateContextStrategy(strategy: ContextStrategy) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(defaultContextStrategy = strategy))
        }
    }

    fun updateContextMaxTokens(value: Int) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(defaultContextMaxTokens = value))
        }
    }

    fun updateSlidingWindowSize(value: Int) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(slidingWindowSize = value))
        }
    }

    fun updateFontSizeScale(value: Float) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(fontSizeScale = value))
        }
    }

    fun updateMaxHistorySessions(value: Int) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateSettingsUseCase(s.copy(maxHistorySessions = value))
        }
    }

    fun showClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    fun hideClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            clearAllSessionsUseCase()
            _uiState.update { it.copy(showClearConfirmDialog = false) }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
