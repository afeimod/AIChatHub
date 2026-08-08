package com.aichathub.domain.repository

import com.aichathub.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务仓库接口
 */
interface AIServiceRepository {
    /**
     * 发送消息并获取 AI 响应（非流式）
     */
    suspend fun sendMessage(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String = "",
        customProvider: CustomProvider? = null
    ): Result<SendMessageResponse>

    /**
     * 流式发送消息，逐 token 返回内容
     * @return Flow<String> 每个 emit 是一段新增的文本
     */
    fun sendMessageStream(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String = "",
        customProvider: CustomProvider? = null
    ): Flow<String>

    /**
     * 测试 API 连接
     */
    suspend fun testConnection(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String,
        customProvider: CustomProvider? = null
    ): Result<Boolean>
}

/**
 * API 密钥仓库接口
 */
interface APIKeyRepository {
    fun getAllAPIKeys(): Flow<List<APIKeyInfo>>
    fun getAPIKeysByPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>>
    fun getActiveAPIKey(): Flow<APIKeyInfo?>
    suspend fun addAPIKey(info: APIKeyInfo)
    suspend fun updateAPIKey(info: APIKeyInfo)
    suspend fun deleteAPIKey(id: String)
    suspend fun setActiveAPIKey(id: String)
    suspend fun getDecryptedAPIKey(id: String): String?
}

/**
 * 对话会话仓库接口
 */
interface ChatSessionRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSession(id: String): ChatSession?
    suspend fun createSession(session: ChatSession): String
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(id: String)
    suspend fun addMessageToSession(sessionId: String, message: ChatMessage)
    suspend fun updateMessage(sessionId: String, message: ChatMessage)
    suspend fun deleteMessage(sessionId: String, messageId: String)
    suspend fun clearAllSessions()
}

/**
 * 应用设置仓库接口
 */
interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun toggleDarkMode()
    suspend fun setDefaultPlatform(platform: AIPlatform)
}

/**
 * 自定义平台仓库接口
 */
interface CustomProviderRepository {
    fun getAllProviders(): Flow<List<CustomProvider>>
    suspend fun getProvider(id: String): CustomProvider?
    suspend fun addProvider(provider: CustomProvider): String
    suspend fun updateProvider(provider: CustomProvider)
    suspend fun deleteProvider(id: String)
}

/**
 * 工作目录仓库接口
 */
interface WorkspaceRepository {
    fun getSettings(): Flow<WorkspaceSettings>
    suspend fun updateSettings(settings: WorkspaceSettings)
    /** 列出工作目录中的文件 */
    suspend fun listFiles(): List<WorkspaceFile>
    /** 从工作目录读取文件为字节数组 */
    suspend fun readFile(fileName: String): ByteArray?
    /** 写入文件到工作目录 */
    suspend fun writeFile(fileName: String, content: ByteArray): Boolean
    /** 删除文件 */
    suspend fun deleteFile(fileName: String): Boolean
}

/**
 * 终端日志仓库接口
 */
interface TerminalLogRepository {
    fun getLogs(): Flow<List<TerminalLog>>
    suspend fun addLog(log: TerminalLog)
    suspend fun addLogs(logs: List<TerminalLog>)
    suspend fun clearLogs()
}
