package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.APIKeyInfo
import com.aichathub.domain.model.CustomProvider
import com.aichathub.domain.repository.CustomProviderRepository
import com.aichathub.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class APIKeyUiState(
    val apiKeys: List<APIKeyInfo> = emptyList(),
    val customProviders: List<CustomProvider> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingKey: APIKeyInfo? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testResultKeyId: String? = null
)

@HiltViewModel
class APIKeyViewModel @Inject constructor(
    private val getAPIKeysUseCase: GetAPIKeysUseCase,
    private val addAPIKeyUseCase: AddAPIKeyUseCase,
    private val updateAPIKeyUseCase: UpdateAPIKeyUseCase,
    private val deleteAPIKeyUseCase: DeleteAPIKeyUseCase,
    private val setActiveAPIKeyUseCase: SetActiveAPIKeyUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val apiKeyRepository: com.aichathub.domain.repository.APIKeyRepository,
    private val customProviderRepository: CustomProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(APIKeyUiState())
    val uiState: StateFlow<APIKeyUiState> = _uiState.asStateFlow()

    init { loadAPIKeys() }

    fun loadAPIKeys() {
        viewModelScope.launch {
            getAPIKeysUseCase().collect { keys ->
                _uiState.update { it.copy(apiKeys = keys) }
            }
        }
        viewModelScope.launch {
            customProviderRepository.getAllProviders().collect { providers ->
                _uiState.update { it.copy(customProviders = providers) }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }

    fun showEditDialog(key: APIKeyInfo) {
        _uiState.update { it.copy(showEditDialog = true, editingKey = key) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingKey = null) }
    }

    fun addAPIKey(
        platform: AIPlatform,
        apiKey: String,
        name: String,
        customEndpoint: String? = null,
        customModels: List<String> = emptyList(),
        customModelOverride: String? = null,
        customProviderId: String? = null
    ) {
        viewModelScope.launch {
            try {
                addAPIKeyUseCase(platform, apiKey, name, customEndpoint, customModels, customModelOverride, customProviderId)
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateAPIKey(
        key: APIKeyInfo,
        newApiKey: String? = null,
        newName: String? = null,
        newEndpoint: String? = null,
        newCustomModels: List<String>? = null,
        newModelOverride: String? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = key.copy(
                    apiKey = newApiKey?.ifBlank { "" } ?: key.apiKey,
                    name = newName ?: key.name,
                    customEndpoint = newEndpoint ?: key.customEndpoint,
                    customModels = newCustomModels ?: key.customModels,
                    customModelOverride = newModelOverride ?: key.customModelOverride
                )
                updateAPIKeyUseCase(updated)
                _uiState.update { it.copy(showEditDialog = false, editingKey = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteAPIKey(id: String) {
        viewModelScope.launch { deleteAPIKeyUseCase(id) }
    }

    fun setActiveAPIKey(id: String) {
        viewModelScope.launch { setActiveAPIKeyUseCase(id) }
    }

    fun testConnection(key: APIKeyInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null, testResultKeyId = key.id) }
            try {
                val apiKey = apiKeyRepository.getDecryptedAPIKey(key.id) ?: throw IllegalStateException("无法解密 API Key")
                val model = key.defaultModel().ifBlank { key.platform.defaultModel }
                val customProvider = key.customProviderId?.let { customProviderRepository.getProvider(it) }
                val result = testConnectionUseCase(
                    platform = key.platform,
                    apiKey = apiKey,
                    endpoint = key.getEndpoint(),
                    model = model,
                    customProvider = customProvider
                )
                val msg = if (result.isSuccess) "✓ 连接成功" else "✗ 连接失败: ${result.exceptionOrNull()?.message}"
                _uiState.update { it.copy(isTesting = false, testResult = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTesting = false, testResult = "✗ 测试失败: ${e.message}") }
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null, testResultKeyId = null) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
