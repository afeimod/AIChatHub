package com.aichathub.domain.usecase

import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.APIKeyInfo
import com.aichathub.domain.model.AppSettings
import com.aichathub.domain.model.ChatMessage
import com.aichathub.domain.model.ChatSession
import com.aichathub.domain.model.MessageRole
import com.aichathub.domain.model.SendMessageResponse
import com.aichathub.domain.repository.AIServiceRepository
import com.aichathub.domain.repository.APIKeyRepository
import com.aichathub.domain.repository.ChatSessionRepository
import com.aichathub.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 发送消息用例
 */
class SendMessageUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository,
    private val apiKeyRepository: APIKeyRepository,
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String,
        platform: AIPlatform,
        model: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Result<SendMessageResponse> {
        // 获取会话
        val session = chatSessionRepository.getSession(sessionId)
            ?: return Result.failure(Exception("会话不存在"))

        // 获取API密钥
        val apiKeyInfoResult = apiKeyRepository.getActiveAPIKey()
        if (apiKeyInfoResult == null) {
            return Result.failure(Exception("请先配置API密钥"))
        }
        val apiKeyInfo: APIKeyInfo = apiKeyInfoResult

        if (apiKeyInfo.platform != platform) {
            return Result.failure(Exception("当前激活的API密钥不匹配所选平台"))
        }

        val keyId = apiKeyInfo.id
        val decryptedKeyResult = apiKeyRepository.getDecryptedAPIKey(keyId)
        if (decryptedKeyResult == null) {
            return Result.failure(Exception("无法获取API密钥"))
        }
        val decryptedKey: String = decryptedKeyResult

        // 构建消息列表
        val messages = session.messages.toMutableList().apply {
            add(
                ChatMessage(
                    role = MessageRole.USER,
                    content = userMessage,
                    platform = platform,
                    model = model
                )
            )
        }

        // 发送请求
        val result = aiServiceRepository.sendMessage(
            platform = platform,
            apiKey = decryptedKey,
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        // 如果成功，保存用户消息和AI响应
        if (result.isSuccess) {
            val response = result.getOrThrow()
            messages.add(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = response.content,
                    platform = platform,
                    model = model
                )
            )

            // 更新会话
            val updatedSession = session.copy(
                messages = messages,
                updatedAt = System.currentTimeMillis()
            )
            chatSessionRepository.updateSession(updatedSession)
        }

        return result
    }
}

/**
 * 创建新对话用例
 */
class CreateSessionUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(title: String = "新对话"): String {
        val session = ChatSession(
            title = title,
            platform = AIPlatform.DEEPSEEK,
            model = AIPlatform.DEEPSEEK.defaultModel
        )
        return chatSessionRepository.createSession(session)
    }
}

/**
 * 获取API密钥列表用例
 */
class GetAPIKeysUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    operator fun invoke(): Flow<List<APIKeyInfo>> {
        return apiKeyRepository.getAllAPIKeys()
    }

    fun byPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>> {
        return apiKeyRepository.getAPIKeysByPlatform(platform)
    }
}

/**
 * 添加API密钥用例
 */
class AddAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(
        platform: AIPlatform,
        apiKey: String,
        name: String
    ): Result<Unit> {
        return try {
            if (apiKey.isBlank()) {
                return Result.failure(Exception("API密钥不能为空"))
            }

            val info = APIKeyInfo(
                id = java.util.UUID.randomUUID().toString(),
                platform = platform,
                apiKey = apiKey,
                name = name.ifBlank { platform.displayName }
            )
            apiKeyRepository.addAPIKey(info)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 删除API密钥用例
 */
class DeleteAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            apiKeyRepository.deleteAPIKey(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 设置活跃API密钥用例
 */
class SetActiveAPIKeyUseCase @Inject constructor(
    private val apiKeyRepository: APIKeyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            apiKeyRepository.setActiveAPIKey(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 测试API连接用例
 */
class TestConnectionUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository
) {
    suspend operator fun invoke(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String
    ): Result<Boolean> {
        return aiServiceRepository.testConnection(
            platform = platform,
            apiKey = apiKey,
            endpoint = endpoint,
            model = model
        )
    }
}

/**
 * 获取设置用例
 */
class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return settingsRepository.getSettings()
    }
}

/**
 * 更新设置用例
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings): Result<Unit> {
        return try {
            settingsRepository.updateSettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 删除对话会话用例
 */
class DeleteSessionUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        return try {
            chatSessionRepository.deleteSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 清空所有对话用例
 */
class ClearAllSessionsUseCase @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            chatSessionRepository.clearAllSessions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}