package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.ApiStyle
import com.aichathub.domain.model.CustomProvider
import com.aichathub.domain.usecase.AddCustomProviderUseCase
import com.aichathub.domain.usecase.DeleteCustomProviderUseCase
import com.aichathub.domain.usecase.GetCustomProvidersUseCase
import com.aichathub.domain.usecase.UpdateCustomProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomProviderUiState(
    val providers: List<CustomProvider> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingProvider: CustomProvider? = null,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class CustomProviderViewModel @Inject constructor(
    private val getCustomProvidersUseCase: GetCustomProvidersUseCase,
    private val addCustomProviderUseCase: AddCustomProviderUseCase,
    private val updateCustomProviderUseCase: UpdateCustomProviderUseCase,
    private val deleteCustomProviderUseCase: DeleteCustomProviderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomProviderUiState())
    val uiState: StateFlow<CustomProviderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCustomProvidersUseCase().collect { providers ->
                _uiState.update { it.copy(providers = providers) }
            }
        }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideDialog() { _uiState.update { it.copy(showAddDialog = false, editingProvider = null) } }

    fun showEditDialog(provider: CustomProvider) {
        _uiState.update { it.copy(editingProvider = provider) }
    }

    fun addProvider(
        name: String,
        endpoint: String,
        apiKey: String,
        models: List<String>,
        defaultModel: String,
        apiStyle: ApiStyle,
        authHeader: String,
        authPrefix: String
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || endpoint.isBlank()) {
                    _uiState.update { it.copy(error = "名称和端点必填") }
                    return@launch
                }
                val provider = CustomProvider(
                    name = name,
                    endpoint = endpoint,
                    apiKey = apiKey,
                    models = models.filter { it.isNotBlank() },
                    defaultModel = defaultModel.ifBlank { models.firstOrNull() ?: "" },
                    apiStyle = apiStyle,
                    authHeader = authHeader.ifBlank { "Authorization" },
                    authPrefix = authPrefix.ifBlank { "Bearer " }
                )
                addCustomProviderUseCase(provider)
                _uiState.update { it.copy(showAddDialog = false, message = "已添加自定义平台: $name") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "添加失败: ${e.message}") }
            }
        }
    }

    fun updateProvider(provider: CustomProvider) {
        viewModelScope.launch {
            try {
                updateCustomProviderUseCase(provider)
                _uiState.update { it.copy(editingProvider = null, message = "已更新: ${provider.name}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "更新失败: ${e.message}") }
            }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            try {
                deleteCustomProviderUseCase(id)
                _uiState.update { it.copy(message = "已删除自定义平台") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(error = null, message = null) } }
}
