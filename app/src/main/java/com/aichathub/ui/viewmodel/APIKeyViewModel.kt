package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.*
import com.aichathub.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class APIKeyUiState(
    val apiKeys: List<APIKeyInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingKey: APIKeyInfo? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null
)

@HiltViewModel
class APIKeyViewModel @Inject constructor(
    private val getAPIKeysUseCase: GetAPIKeysUseCase,
    private val addAPIKeyUseCase: AddAPIKeyUseCase,
    private val deleteAPIKeyUseCase: DeleteAPIKeyUseCase,
    private val setActiveAPIKeyUseCase: SetActiveAPIKeyUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(APIKeyUiState())
    val uiState: StateFlow<APIKeyUiState> = _uiState.asStateFlow()

    init {
        loadAPIKeys()
    }

    private fun loadAPIKeys() {
        viewModelScope.launch {
            getAPIKeysUseCase().collect { keys ->
                _uiState.update { it.copy(apiKeys = keys) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun showEditDialog(key: APIKeyInfo) {
        _uiState.update { it.copy(showEditDialog = true, editingKey = key) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingKey = null) }
    }

    fun addAPIKey(platform: AIPlatform, apiKey: String, name: String, customEndpoint: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 创建带自定义端点的 APIKeyInfo
            val keyInfo = APIKeyInfo(
                id = java.util.UUID.randomUUID().toString(),
                platform = platform,
                apiKey = apiKey,
                name = name.ifBlank { platform.displayName },
                customEndpoint = customEndpoint
            )

            val result = addAPIKeyUseCase(platform, apiKey, name)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "添加失败")
                    }
                }
            )
        }
    }

    fun deleteAPIKey(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deleteAPIKeyUseCase(id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "删除失败")
                    }
                }
            )
        }
    }

    fun setActiveAPIKey(id: String) {
        viewModelScope.launch {
            val result = setActiveAPIKeyUseCase(id)
            result.fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "设置失败")
                    }
                }
            )
        }
    }

    fun testConnection(platform: AIPlatform, apiKey: String, endpoint: String, model: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val result = testConnectionUseCase(
                platform = platform,
                apiKey = apiKey,
                endpoint = endpoint,
                model = model
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isTesting = false, testResult = "连接成功!")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isTesting = false, testResult = "连接失败: ${error.message}")
                    }
                }
            )
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}