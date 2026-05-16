package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.*
import com.aichathub.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val clearAllSessionsUseCase: ClearAllSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = currentSettings.copy(isDarkMode = !currentSettings.isDarkMode)
            val result = updateSettingsUseCase(newSettings)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun updateDefaultPlatform(platform: AIPlatform) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = currentSettings.copy(defaultPlatform = platform)
            val result = updateSettingsUseCase(newSettings)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun updateDefaultTemperature(temperature: Float) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = currentSettings.copy(defaultTemperature = temperature)
            val result = updateSettingsUseCase(newSettings)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun updateDefaultMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = currentSettings.copy(defaultMaxTokens = maxTokens)
            val result = updateSettingsUseCase(newSettings)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun toggleStreamResponse() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = currentSettings.copy(enableStreamResponse = !currentSettings.enableStreamResponse)
            val result = updateSettingsUseCase(newSettings)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
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
            _uiState.update { it.copy(isLoading = true) }
            val result = clearAllSessionsUseCase()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, showClearConfirmDialog = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}