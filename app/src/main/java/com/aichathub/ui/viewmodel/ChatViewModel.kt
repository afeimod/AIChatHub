package com.aichathub.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.APIKeyRepository
import com.aichathub.domain.repository.ChatSessionRepository
import com.aichathub.domain.repository.SettingsRepository
import com.aichathub.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Base64
import javax.inject.Inject

data class ChatUiState(
    val currentSession: ChatSession? = null,
    val allSessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val selectedPlatform: AIPlatform = AIPlatform.DEEPSEEK,
    val selectedModel: String = AIPlatform.DEEPSEEK.defaultModel,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isTyping: Boolean = false,
    val error: String? = null,
    val activeAPIKey: APIKeyInfo? = null,
    val settings: AppSettings = AppSettings(),
    val showAPIKeyDialog: Boolean = false,
    val pendingAttachments: List<MessageAttachment> = emptyList()  // 待发送的附件
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val getAPIKeysUseCase: GetAPIKeysUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val clearAllSessionsUseCase: ClearAllSessionsUseCase,
    private val chatSessionRepository: ChatSessionRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 加载设置（用于默认值）
            getSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        // 仅当没有选择平台时使用设置中的默认值
                        selectedPlatform = if (it.selectedPlatform == AIPlatform.DEEPSEEK) settings.defaultPlatform else it.selectedPlatform,
                        selectedModel = if (it.selectedModel == AIPlatform.DEEPSEEK.defaultModel) settings.defaultPlatform.defaultModel else it.selectedModel
                    )
                }
            }
        }

        viewModelScope.launch {
            // 加载活跃的API密钥
            apiKeyRepository.getActiveAPIKey().collect { activeKey ->
                if (activeKey != null) {
                    _uiState.update {
                        it.copy(
                            activeAPIKey = activeKey,
                            // 如果没有用户手动选择，使用API密钥对应的平台
                            selectedPlatform = if (it.selectedPlatform == AIPlatform.DEEPSEEK && it.messages.isEmpty()) activeKey.platform else it.selectedPlatform,
                            selectedModel = if (it.selectedModel == AIPlatform.DEEPSEEK.defaultModel && it.messages.isEmpty()) activeKey.platform.defaultModel else it.selectedModel
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            // 加载所有会话
            chatSessionRepository.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(allSessions = sessions) }
                // 如果没有当前会话且有会话列表，选择第一个
                if (_uiState.value.currentSession == null && sessions.isNotEmpty()) {
                    val firstSession = sessions.first()
                    _uiState.update {
                        it.copy(
                            currentSession = firstSession,
                            messages = firstSession.messages,
                            selectedPlatform = firstSession.platform,
                            selectedModel = firstSession.model
                        )
                    }
                }
            }
        }
    }

    /**
     * 添加附件到待发送列表
     */
    fun addAttachment(uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
            val fileSize = getFileSize(uri)

            // 读取文件内容并转换为Base64（对于小文件）
            val base64Data = if (fileSize < 5 * 1024 * 1024) { // 小于5MB的文件使用Base64
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    Base64.getEncoder().encodeToString(bytes)
                }
            } else {
                null // 大文件使用本地路径
            }

            val attachment = MessageAttachment(
                fileName = fileName,
                mimeType = mimeType,
                size = fileSize,
                type = determineAttachmentType(mimeType),
                localPath = uri.toString(),
                base64Data = base64Data
            )

            _uiState.update {
                it.copy(pendingAttachments = it.pendingAttachments + attachment)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "添加附件失败: ${e.message}")
            }
        }
    }

    /**
     * 移除附件
     */
    fun removeAttachment(attachment: MessageAttachment) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filter { a -> a.id != attachment.id })
        }
    }

    /**
     * 清空所有待发送附件
     */
    fun clearAttachments() {
        _uiState.update { it.copy(pendingAttachments = emptyList()) }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !it.isNull(sizeIndex)) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        }
        return size
    }

    private fun determineAttachmentType(mimeType: String): AttachmentType {
        return when {
            mimeType.startsWith("image/") -> AttachmentType.IMAGE
            mimeType == "application/pdf" -> AttachmentType.PDF
            mimeType.contains("archive") || mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("compressed") -> AttachmentType.ARCHIVE
            mimeType.contains("document") || mimeType.contains("text") -> AttachmentType.DOCUMENT
            else -> AttachmentType.OTHER
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val session = ChatSession(
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel
            )
            val id = chatSessionRepository.createSession(session)
            val newSession = session.copy(id = id)
            _uiState.update {
                it.copy(
                    currentSession = newSession,
                    messages = emptyList()
                )
            }
        }
    }

    fun selectSession(session: ChatSession) {
        _uiState.update {
            it.copy(
                currentSession = session,
                messages = session.messages,
                selectedPlatform = session.platform,
                selectedModel = session.model
            )
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            deleteSessionUseCase(sessionId)
            if (_uiState.value.currentSession?.id == sessionId) {
                _uiState.update {
                    it.copy(
                        currentSession = null,
                        messages = emptyList()
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val attachments = _uiState.value.pendingAttachments

        // 至少要有文本或附件才能发送
        if (text.isBlank() && attachments.isEmpty()) return

        val session = _uiState.value.currentSession
        if (session == null) {
            createNewSession()
            viewModelScope.launch {
                kotlinx.coroutines.delay(100)
                sendMessageActual(text, attachments)
            }
            return
        }

        sendMessageActual(text, attachments)
    }

    private fun sendMessageActual(text: String, attachments: List<MessageAttachment>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    inputText = "",
                    isLoading = true,
                    isTyping = true,
                    error = null,
                    pendingAttachments = emptyList() // 清空附件
                )
            }

            // 创建用户消息（包含附件）
            val userMessage = ChatMessage(
                role = MessageRole.USER,
                content = text,
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel,
                attachments = attachments
            )

            val updatedMessages = _uiState.value.messages + userMessage
            _uiState.update { it.copy(messages = updatedMessages) }

            val session = _uiState.value.currentSession ?: return@launch

            val result = sendMessageUseCase(
                sessionId = session.id,
                userMessage = text,
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel,
                temperature = _uiState.value.settings.defaultTemperature,
                maxTokens = _uiState.value.settings.defaultMaxTokens,
                attachments = attachments
            )

            result.fold(
                onSuccess = { response ->
                    val assistantMessage = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = response.content,
                        platform = response.platform,
                        model = response.model
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false,
                            isTyping = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isTyping = false,
                            error = error.message ?: "发送消息失败"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun selectPlatform(platform: AIPlatform) {
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                selectedModel = platform.defaultModel
            )
        }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun getAvailableModels(platform: AIPlatform): List<String> {
        return platform.models.ifEmpty { listOf(platform.defaultModel) }
    }
}