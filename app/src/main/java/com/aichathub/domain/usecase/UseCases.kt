package com.aichathub.domain.usecase

import com.aichathub.domain.model.*
import com.aichathub.domain.repository.*
import com.aichathub.domain.util.ContextManager
import com.aichathub.domain.util.TokenEstimator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// ==================== 会话 ====================

class CreateSessionUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        title: String = "新对话",
        platform: AIPlatform? = null,
        model: String? = null,
        systemPrompt: String = ""
    ): String {
        val settings = settingsRepository.getSettings().first()
        val p = platform ?: settings.defaultPlatform
        val m = model ?: p.defaultModel
        val session = ChatSession(
            title = title,
            platform = p,
            model = m,
            systemPrompt = systemPrompt,
            contextStrategy = settings.defaultContextStrategy,
            contextMaxTokens = settings.defaultContextMaxTokens
        )
        return chatSessionRepository.createSession(session)
    }
}

class DeleteSessionUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(id: String) = chatSessionRepository.deleteSession(id)
}

class ClearAllSessionsUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke() = chatSessionRepository.clearAllSessions()
}

class UpdateSessionUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(session: ChatSession) = chatSessionRepository.updateSession(session)
}

class DeleteMessageUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(sessionId: String, messageId: String) =
        chatSessionRepository.deleteMessage(sessionId, messageId)
}

class UpdateMessageUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(sessionId: String, message: ChatMessage) =
        chatSessionRepository.updateMessage(sessionId, message)
}

// ==================== API Key ====================

class GetAPIKeysUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    operator fun invoke(): Flow<List<APIKeyInfo>> = apiKeyRepository.getAllAPIKeys()

    fun byPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>> =
        apiKeyRepository.getAPIKeysByPlatform(platform)
}

class AddAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(
        platform: AIPlatform,
        apiKey: String,
        name: String,
        customEndpoint: String? = null,
        customModels: List<String> = emptyList(),
        customModelOverride: String? = null,
        customProviderId: String? = null
    ) {
        val info = APIKeyInfo(
            id = java.util.UUID.randomUUID().toString(),
            platform = platform,
            apiKey = apiKey,
            name = name.ifBlank { platform.displayName },
            customEndpoint = customEndpoint?.ifBlank { null },
            customModels = customModels,
            customModelOverride = customModelOverride,
            customProviderId = customProviderId,
            isActive = false,
            createdAt = System.currentTimeMillis()
        )
        apiKeyRepository.addAPIKey(info)
    }
}

class UpdateAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(info: APIKeyInfo) = apiKeyRepository.updateAPIKey(info)
}

class DeleteAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(id: String) = apiKeyRepository.deleteAPIKey(id)
}

class SetActiveAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(id: String) = apiKeyRepository.setActiveAPIKey(id)
}

class TestConnectionUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val customProviderRepository: CustomProviderRepository
) {
    suspend operator fun invoke(
        platform: AIPlatform,
        keyId: String,
        endpoint: String,
        model: String
    ): Result<Boolean> {
        val apiKey = apiKeyRepository.getDecryptedAPIKey(keyId) ?: return Result.failure(IllegalStateException("Key not found"))
        val customProvider = keyId.let { id ->
            runCatching { customProviderRepository.getProvider(id) }.getOrNull()
        }
        // 当 keyId 是 APIKeyInfo 而非 CustomProvider 时，customProvider 为 null
        return aiServiceRepository.testConnection(platform, apiKey, endpoint, model, customProvider = null)
    }

    suspend operator fun invoke(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String,
        customProvider: CustomProvider? = null
    ): Result<Boolean> = aiServiceRepository.testConnection(platform, apiKey, endpoint, model, customProvider)
}

// ==================== 发送消息 ====================

class SendMessageUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val customProviderRepository: CustomProviderRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        userMessage: ChatMessage,
        platform: AIPlatform,
        model: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096,
        attachments: List<MessageAttachment> = emptyList(),
        systemPrompt: String = "",
        customProviderId: String? = null
    ): Result<SendMessageResponse> {
        val session = chatSessionRepository.getSession(sessionId) ?: return Result.failure(IllegalStateException("Session not found"))
        val activeKey = apiKeyRepository.getActiveAPIKey().first() ?: return Result.failure(IllegalStateException("No active API key"))
        val apiKey = apiKeyRepository.getDecryptedAPIKey(activeKey.id) ?: return Result.failure(IllegalStateException("Cannot decrypt API key"))

        val customProvider = if (customProviderId != null) customProviderRepository.getProvider(customProviderId) else null

        val updatedMessages = session.messages + userMessage
        val updatedSession = session.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis())
        chatSessionRepository.updateSession(updatedSession)

        val trimmedMessages = ContextManager.trim(updatedSession)

        return aiServiceRepository.sendMessage(
            platform = platform,
            apiKey = apiKey,
            model = model,
            endpoint = activeKey.getEndpoint(),
            messages = trimmedMessages,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt.ifBlank { session.systemPrompt },
            customProvider = customProvider
        )
    }
}

class SendMessageStreamUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val customProviderRepository: CustomProviderRepository
) {
    operator fun invoke(
        messages: List<ChatMessage>,
        platform: AIPlatform,
        model: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096,
        endpoint: String,
        systemPrompt: String = "",
        customProviderId: String? = null
    ): Flow<String> {
        return kotlinx.coroutines.flow.flow {
            val activeKey = apiKeyRepository.getActiveAPIKey().first() ?: throw IllegalStateException("No active API key")
            val apiKey = apiKeyRepository.getDecryptedAPIKey(activeKey.id) ?: throw IllegalStateException("Cannot decrypt API key")
            val customProvider = if (customProviderId != null) customProviderRepository.getProvider(customProviderId) else null
            val trimmed = if (messages.size > 50) messages.takeLast(50) else messages
            aiServiceRepository.sendMessageStream(
                platform = platform,
                apiKey = apiKey,
                model = model,
                endpoint = endpoint,
                messages = trimmed,
                temperature = temperature,
                maxTokens = maxTokens,
                systemPrompt = systemPrompt,
                customProvider = customProvider
            ).collect { emit(it) }
        }
    }
}

// ==================== 设置 ====================

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) = settingsRepository.updateSettings(settings)
}

// ==================== 自定义平台 ====================

class GetCustomProvidersUseCase @Inject constructor(
    private val customProviderRepository: CustomProviderRepository
) {
    operator fun invoke(): Flow<List<CustomProvider>> = customProviderRepository.getAllProviders()
}

class AddCustomProviderUseCase @Inject constructor(
    private val customProviderRepository: CustomProviderRepository
) {
    suspend operator fun invoke(provider: CustomProvider): String = customProviderRepository.addProvider(provider)
}

class UpdateCustomProviderUseCase @Inject constructor(
    private val customProviderRepository: CustomProviderRepository
) {
    suspend operator fun invoke(provider: CustomProvider) = customProviderRepository.updateProvider(provider)
}

class DeleteCustomProviderUseCase @Inject constructor(
    private val customProviderRepository: CustomProviderRepository
) {
    suspend operator fun invoke(id: String) = customProviderRepository.deleteProvider(id)
}

// ==================== 工作目录 ====================

class GetWorkspaceSettingsUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    operator fun invoke(): Flow<WorkspaceSettings> = workspaceRepository.getSettings()
}

class UpdateWorkspaceSettingsUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(settings: WorkspaceSettings) = workspaceRepository.updateSettings(settings)
}

class ListWorkspaceFilesUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(): List<WorkspaceFile> = workspaceRepository.listFiles()
}

class WriteWorkspaceFileUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(fileName: String, content: ByteArray): Boolean =
        workspaceRepository.writeFile(fileName, content)
}

class WriteWorkspaceTextUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(fileName: String, text: String): Boolean =
        workspaceRepository.writeFile(fileName, text.toByteArray())
}

class DeleteWorkspaceFileUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(fileName: String): Boolean = workspaceRepository.deleteFile(fileName)
}

class ReadWorkspaceFileUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(fileName: String): ByteArray? = workspaceRepository.readFile(fileName)
}

// ==================== 终端日志 ====================

class GetTerminalLogsUseCase @Inject constructor(
    private val terminalLogRepository: TerminalLogRepository
) {
    operator fun invoke(): Flow<List<TerminalLog>> = terminalLogRepository.getLogs()
}

class ClearTerminalLogsUseCase @Inject constructor(
    private val terminalLogRepository: TerminalLogRepository
) {
    suspend operator fun invoke() = terminalLogRepository.clearLogs()
}

// ==================== Token 估算 ====================

class EstimateTokensUseCase @Inject constructor() {
    operator fun invoke(text: String): Int = TokenEstimator.estimateText(text)
    operator fun invoke(session: ChatSession): Int = TokenEstimator.estimateSession(session)
}
