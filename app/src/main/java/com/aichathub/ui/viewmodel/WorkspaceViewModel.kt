package com.aichathub.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.WorkspaceFile
import com.aichathub.domain.model.WorkspaceSettings
import com.aichathub.domain.repository.WorkspaceRepository
import com.aichathub.domain.usecase.DeleteWorkspaceFileUseCase
import com.aichathub.domain.usecase.GetWorkspaceSettingsUseCase
import com.aichathub.domain.usecase.ListWorkspaceFilesUseCase
import com.aichathub.domain.usecase.UpdateWorkspaceSettingsUseCase
import com.aichathub.domain.usecase.WriteWorkspaceTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val settings: WorkspaceSettings = WorkspaceSettings(),
    val files: List<WorkspaceFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val showSaveDialog: Boolean = false,
    val pendingSaveContent: String = ""
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val getWorkspaceSettingsUseCase: GetWorkspaceSettingsUseCase,
    private val updateWorkspaceSettingsUseCase: UpdateWorkspaceSettingsUseCase,
    private val listWorkspaceFilesUseCase: ListWorkspaceFilesUseCase,
    private val writeWorkspaceTextUseCase: WriteWorkspaceTextUseCase,
    private val deleteWorkspaceFileUseCase: DeleteWorkspaceFileUseCase,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getWorkspaceSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                refreshFiles()
            }
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val files = listWorkspaceFilesUseCase()
                _uiState.update { it.copy(files = files, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "读取文件列表失败: ${e.message}") }
            }
        }
    }

    fun setUseDefaultDownload(useDefault: Boolean) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateWorkspaceSettingsUseCase(s.copy(useDefaultDownload = useDefault))
            _uiState.update { it.copy(message = "已切换至${if (useDefault) "默认下载目录" else "自定义目录"}") }
        }
    }

    fun setCustomDirectory(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateWorkspaceSettingsUseCase(
                s.copy(useDefaultDownload = false, customTreeUri = uri.toString(), customDisplayName = displayName)
            )
            _uiState.update { it.copy(message = "已设置自定义目录: $displayName") }
        }
    }

    fun setAutoSave(autoSave: Boolean) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateWorkspaceSettingsUseCase(s.copy(autoSaveAIResponse = autoSave))
        }
    }

    fun setFilePrefix(prefix: String) {
        viewModelScope.launch {
            val s = _uiState.value.settings
            updateWorkspaceSettingsUseCase(s.copy(filePrefix = prefix))
        }
    }

    fun showSaveDialog(content: String) {
        _uiState.update { it.copy(showSaveDialog = true, pendingSaveContent = content) }
    }

    fun hideSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false, pendingSaveContent = "") }
    }

    fun saveTextFile(fileName: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val fullContent = state.pendingSaveContent
            val finalName = if (fileName.isBlank()) "aichathub_${System.currentTimeMillis()}.txt" else fileName
            try {
                val ok = writeWorkspaceTextUseCase(finalName, fullContent)
                if (ok) {
                    _uiState.update { it.copy(showSaveDialog = false, message = "已保存: $finalName", pendingSaveContent = "") }
                    refreshFiles()
                } else {
                    _uiState.update { it.copy(error = "保存失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }

    fun deleteFile(name: String) {
        viewModelScope.launch {
            try {
                val ok = deleteWorkspaceFileUseCase(name)
                if (ok) {
                    _uiState.update { it.copy(message = "已删除: $name") }
                    refreshFiles()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
