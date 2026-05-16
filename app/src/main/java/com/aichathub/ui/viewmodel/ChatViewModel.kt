package com.aichathub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.APIKeyRepository
import com.aichathub.domain.repository.ChatSessionRepository
import com.aichathub.domain.repository.SettingsRepository
import com.aichathub.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val showAPIKeyDialog: Boolean = false
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 加载活跃的API密钥
            apiKeyRepository.getActiveAPIKey().collect { activeKey ->
                _uiState.update {
                    it.copy(
                        activeAPIKey = activeKey,
                        selectedPlatform = activeKey?.platform ?: it.selectedPlatform,
                        selectedModel = activeKey?.platform?.defaultModel ?: it.selectedModel
                    )
                }
            }
        }

        viewModelScope.launch {
            // 加载设置
            getSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        selectedPlatform = settings.defaultPlatform,
                        selectedModel = settings.defaultPlatform.defaultModel
                    )
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
        if (text.isBlank()) return

        val session = _uiState.value.currentSession
        if (session == null) {
            createNewSession()
            // 等待新会话创建完成后再发送
            viewModelScope.launch {
                // 等待一下让新会话创建
                kotlinx.coroutines.delay(100)
                sendMessageActual(text)
            }
            return
        }

        sendMessageActual(text)
    }

    private fun sendMessageActual(text: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    inputText = "",
                    isLoading = true,
                    isTyping = true,
                    error = null
                )
            }

            val userMessage = ChatMessage(
                role = MessageRole.USER,
                content = text,
                platform = _uiState.value.selectedPlatform,
                model = _uiState.value.selectedModel
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
                maxTokens = _uiState.value.settings.defaultMaxTokens
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