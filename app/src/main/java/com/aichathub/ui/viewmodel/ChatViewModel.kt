package com.aichathub.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.data.local.TerminalLogManager
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.APIKeyRepository
import com.aichathub.domain.repository.ChatSessionRepository
import com.aichathub.domain.repository.CustomProviderRepository
import com.aichathub.domain.repository.SettingsRepository
import com.aichathub.domain.usecase.*
import com.aichathub.domain.util.ContextManager
import com.aichathub.domain.util.TokenEstimator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    val isStreaming: Boolean = false,
    val isTyping: Boolean = false,
    val error: String? = null,
    val activeAPIKey: APIKeyInfo? = null,
    val settings: AppSettings = AppSettings(),
    val showAPIKeyDialog: Boolean = false,
    val pendingAttachments: List<MessageAttachment> = emptyList(),
    val showSessionHistory: Boolean = false,
    val showSystemPromptDialog: Boolean = false,
    val systemPrompt: String = "",
    val estimatedInputTokens: Int = 0,
    val estimatedSessionTokens: Int = 0,
    val showWorkspacePicker: Boolean = false,
    val availableModels: List<String> = emptyList(),
    val customProviders: List<CustomProvider> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageStreamUseCase: SendMessageStreamUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val clearAllSessionsUseCase: ClearAllSessionsUseCase,
    private val updateSessionUseCase: UpdateSessionUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val updateMessageUseCase: UpdateMessageUseCase,
    private val getAPIKeysUseCase: GetAPIKeysUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getCustomProvidersUseCase: GetCustomProvidersUseCase,
    private val chatSessionRepository: ChatSessionRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val settingsRepository: SettingsRepository,
    private val customProviderRepository: CustomProviderRepository,
    private val logManager: TerminalLogManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 设置
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            // 活跃 API Key
            apiKeyRepository.getActiveAPIKey().collect { key ->
                _uiState.update {
                    it.copy(
                        activeAPIKey = key,
                        availableModels = key?.availableModels() ?: it.selectedPlatform.models,
                        selectedPlatform = key?.platform ?: it.selectedPlatform,
                        selectedModel = key?.defaultModel() ?: it.selectedPlatform.defaultModel
                    )
                }
            }
        }
        viewModelScope.launch {
            // 所有会话
            chatSessionRepository.getAllSessions().collect { sessions ->
                val current = _uiState.value.currentSession
                val newCurrent = if (current == null && sessions.isNotEmpty()) sessions.first() else current
                _uiState.update {
                    it.copy(
                        allSessions = sessions,
                        currentSession = newCurrent,
                        messages = newCurrent?.messages ?: emptyList(),
                        systemPrompt = newCurrent?.systemPrompt ?: it.systemPrompt,
                        estimatedSessionTokens = newCurrent?.let { s -> TokenEstimator.estimateSession(s) } ?: 0
                    )
                }
            }
        }
        viewModelScope.launch {
            // 自定义平台
            getCustomProvidersUseCase().collect { providers ->
                _uiState.update { it.copy(customProviders = providers) }
            }
        }
    }

    // ============ 输入与附件 ============

    fun updateInputText(text: String) {
        val tokens = TokenEstimator.estimateText(text)
        _uiState.update { it.copy(inputText = text, estimatedInputTokens = tokens) }
    }

    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                val (displayName, size) = queryFileInfo(uri)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val base64 = Base64.getEncoder().encodeToString(bytes)
                val attachment = MessageAttachment(
                    fileName = displayName,
                    mimeType = mimeType,
                    size = size,
                    type = MessageAttachment.fromMimeType(mimeType, displayName, size).type,
                    base64Data = base64,
                    localPath = uri.toString()
                )
                logManager.info("Chat", "附件已添加: $displayName ($mimeType, ${size}B)")
                _uiState.update {
                    it.copy(pendingAttachments = it.pendingAttachments + attachment)
                }
            } catch (e: Exception) {
                logManager.error("Chat", "读取附件失败: ${e.message}")
                _uiState.update { it.copy(error = "读取附件失败: ${e.message}") }
            }
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "attachment"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx)
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    fun removeAttachment(id: String) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments.filterNot { att -> att.id == id })
        }
    }

    fun clearAttachments() {
        _uiState.update { it.copy(pendingAttachments = emptyList()) }
    }

    // ============ 会话管理 ============

    fun createNewSession() {
        viewModelScope.launch {
            val title = if (_uiState.value.settings.autoTitleFromFirstMessage) "新对话" else "新对话"
            val id = createSessionUseCase(
                title = title,
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel,
                systemPrompt = _uiState.value.systemPrompt
            )
            val session = chatSessionRepository.getSession(id)
            if (session != null) {
                _uiState.update {
                    it.copy(
                        currentSession = session,
                        messages = session.messages,
                        systemPrompt = session.systemPrompt,
                        inputText = "",
                        pendingAttachments = emptyList(),
                        error = null
                    )
                }
            }
            logManager.info("Chat", "创建新会话: $id")
        }
    }

    fun selectSession(session: ChatSession) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentSession = session,
                    messages = session.messages,
                    systemPrompt = session.systemPrompt,
                    selectedPlatform = session.platform,
                    selectedModel = session.model,
                    showSessionHistory = false,
                    estimatedSessionTokens = TokenEstimator.estimateSession(session)
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            deleteSessionUseCase(sessionId)
            if (_uiState.value.currentSession?.id == sessionId) {
                _uiState.update {
                    it.copy(currentSession = null, messages = emptyList(), systemPrompt = "")
                }
            }
        }
    }

    fun showSessionHistory(show: Boolean) {
        _uiState.update { it.copy(showSessionHistory = show) }
    }

    fun showSystemPromptDialog(show: Boolean) {
        _uiState.update { it.copy(showSystemPromptDialog = show) }
    }

    fun updateSystemPrompt(prompt: String) {
        _uiState.update { it.copy(systemPrompt = prompt) }
        viewModelScope.launch {
            _uiState.value.currentSession?.let { session ->
                val updated = session.copy(systemPrompt = prompt, updatedAt = System.currentTimeMillis())
                updateSessionUseCase(updated)
                _uiState.update { it.copy(currentSession = updated) }
            }
        }
    }

    // ============ 平台与模型选择 ============

    fun selectPlatform(platform: AIPlatform) {
        val model = _uiState.value.activeAPIKey?.defaultModel() ?: platform.defaultModel
        val available = _uiState.value.activeAPIKey?.availableModels()?.ifEmpty { platform.models } ?: platform.models
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                selectedModel = model,
                availableModels = available
            )
        }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun getAvailableModels(): List<String> {
        return _uiState.value.availableModels.ifEmpty {
            _uiState.value.selectedPlatform.models
        }
    }

    // ============ 发送消息（含流式） ============

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val attachments = state.pendingAttachments
        if (text.isBlank() && attachments.isEmpty()) return
        if (state.isLoading || state.isStreaming) return

        viewModelScope.launch {
            val sessionId = state.currentSession?.id ?: run {
                val id = createSessionUseCase(
                    platform = state.selectedPlatform,
                    model = state.selectedModel,
                    systemPrompt = state.systemPrompt
                )
                kotlinx.coroutines.delay(50)
                id
            }

            val userMessage = ChatMessage(
                role = MessageRole.USER,
                content = text,
                platform = state.selectedPlatform,
                model = state.selectedModel,
                attachments = attachments
            )

            // 更新 UI 并清空输入
            _uiState.update {
                it.copy(
                    inputText = "",
                    pendingAttachments = emptyList(),
                    isLoading = true,
                    isTyping = true,
                    error = null,
                    estimatedInputTokens = 0
                )
            }

            // 流式或非流式
            // 注意：非流式路径由 SendMessageUseCase 内部负责持久化用户消息；
            // 流式路径需要手动持久化（因 SendMessageStreamUseCase 不写库）
            val settings = state.settings
            if (settings.enableStreamResponse) {
                chatSessionRepository.addMessageToSession(sessionId, userMessage)
                sendStreaming(sessionId, userMessage, settings)
            } else {
                sendNonStreaming(sessionId, userMessage, settings)
            }
        }
    }

    private fun sendStreaming(sessionId: String, userMessage: ChatMessage, settings: AppSettings) {
        streamingJob = viewModelScope.launch {
            // 创建占位 assistant 消息
            val placeholder = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel,
                isStreaming = true
            )
            chatSessionRepository.addMessageToSession(sessionId, placeholder)
            _uiState.update { it.copy(isStreaming = true, isLoading = false, isTyping = true) }

            val accumulated = StringBuilder()
            val session = chatSessionRepository.getSession(sessionId) ?: return@launch
            val trimmedMessages = ContextManager.trim(session).filter { it.id != placeholder.id }

            try {
                sendMessageStreamUseCase(
                    messages = trimmedMessages,
                    platform = _uiState.value.selectedPlatform,
                    model = _uiState.value.selectedModel,
                    temperature = settings.defaultTemperature,
                    maxTokens = settings.defaultMaxTokens,
                    endpoint = _uiState.value.activeAPIKey?.getEndpoint() ?: _uiState.value.selectedPlatform.defaultEndpoint,
                    systemPrompt = _uiState.value.systemPrompt.ifBlank { session.systemPrompt }
                ).collect { chunk ->
                    accumulated.append(chunk)
                    val updatedMsg = placeholder.copy(content = accumulated.toString())
                    updateMessageInUiAndDb(sessionId, updatedMsg)
                }

                // 完成
                val finalMsg = placeholder.copy(content = accumulated.toString(), isStreaming = false)
                updateMessageInUiAndDb(sessionId, finalMsg)
                logManager.info("Chat", "流式完成: ${accumulated.length} chars")
            } catch (e: Exception) {
                val errMsg = placeholder.copy(
                    content = if (accumulated.isEmpty()) "请求失败: ${e.message}" else accumulated.toString(),
                    isStreaming = false,
                    isError = accumulated.isEmpty()
                )
                updateMessageInUiAndDb(sessionId, errMsg)
                _uiState.update { it.copy(error = e.message) }
                logManager.error("Chat", "流式失败: ${e.message}")
            } finally {
                _uiState.update { it.copy(isStreaming = false, isLoading = false, isTyping = false) }
            }
        }
    }

    private suspend fun updateMessageInUiAndDb(sessionId: String, message: ChatMessage) {
        val session = chatSessionRepository.getSession(sessionId) ?: return
        val updatedMessages = session.messages.map { if (it.id == message.id) message else it }
        val updatedSession = session.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
        chatSessionRepository.updateSession(updatedSession)
        _uiState.update {
            it.copy(
                currentSession = updatedSession,
                messages = updatedMessages,
                estimatedSessionTokens = TokenEstimator.estimateSession(updatedSession)
            )
        }
    }

    private fun sendNonStreaming(sessionId: String, userMessage: ChatMessage, settings: AppSettings) {
        viewModelScope.launch {
            val result = sendMessageUseCase(
                sessionId = sessionId,
                userMessage = userMessage,
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel,
                temperature = settings.defaultTemperature,
                maxTokens = settings.defaultMaxTokens,
                attachments = userMessage.attachments,
                systemPrompt = _uiState.value.systemPrompt
            )

            result.onSuccess { resp ->
                val assistantMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = resp.content,
                    platform = resp.platform,
                    model = resp.model,
                    usage = resp.usage
                )
                chatSessionRepository.addMessageToSession(sessionId, assistantMsg)
                logManager.info("Chat", "回复完成: ${resp.content.length} chars")
            }.onFailure { e ->
                val errMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "请求失败: ${e.message}",
                    isError = true
                )
                chatSessionRepository.addMessageToSession(sessionId, errMsg)
                _uiState.update { it.copy(error = e.message) }
                logManager.error("Chat", "请求失败: ${e.message}")
            }
            _uiState.update { it.copy(isLoading = false, isTyping = false) }
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update { it.copy(isStreaming = false, isLoading = false, isTyping = false) }
        logManager.warn("Chat", "用户停止生成")
    }

    // ============ 消息操作 ============

    fun deleteMessage(messageId: String) {
        val sessionId = _uiState.value.currentSession?.id ?: return
        viewModelScope.launch {
            deleteMessageUseCase(sessionId, messageId)
            val session = chatSessionRepository.getSession(sessionId)
            if (session != null) {
                _uiState.update {
                    it.copy(currentSession = session, messages = session.messages)
                }
            }
        }
    }

    fun regenerateMessage(messageId: String) {
        val state = _uiState.value
        val session = state.currentSession ?: return
        val messages = session.messages
        val targetIdx = messages.indexOfFirst { it.id == messageId }
        if (targetIdx < 0) return

        // 找到上一条用户消息
        val lastUserIdx = (0 until targetIdx).lastOrNull { messages[it].role == MessageRole.USER } ?: return
        val lastUserMsg = messages[lastUserIdx]

        // 删除原回复
        viewModelScope.launch {
            deleteMessageUseCase(session.id, messageId)
            // 将上一条用户消息重新作为输入
            _uiState.update {
                it.copy(inputText = lastUserMsg.content, pendingAttachments = lastUserMsg.attachments)
            }
            // 删除原 user 消息（因为 sendMessage 会再次添加）
            deleteMessageUseCase(session.id, lastUserMsg.id)
            sendMessage()
        }
    }

    fun copyMessage(content: String) {
        // 实际复制操作由 UI 层 ClipboardManager 完成；此处仅记录日志
        logManager.info("Chat", "已复制消息 (${content.length} chars)")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showAPIKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showAPIKeyDialog = show) }
    }

    /** 根据当前会话生成会话标题 */
    fun autoGenerateTitle() {
        val session = _uiState.value.currentSession ?: return
        val firstUserMsg = session.messages.firstOrNull { it.role == MessageRole.USER }
        if (firstUserMsg != null && session.title == "新对话") {
            val newTitle = firstUserMsg.content.take(20).replace("\n", " ").trim().ifBlank { "新对话" }
            viewModelScope.launch {
                updateSessionUseCase(session.copy(title = newTitle))
            }
        }
    }
}
