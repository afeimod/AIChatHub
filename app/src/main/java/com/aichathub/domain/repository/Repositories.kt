package com.aichathub.domain.repository

import com.aichathub.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * AI服务仓库接口
 */
interface AIServiceRepository {
    /**
     * 发送消息并获取AI响应
     */
    suspend fun sendMessage(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): Result<SendMessageResponse>

    /**
     * 测试API连接
     */
    suspend fun testConnection(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String
    ): Result<Boolean>
}

/**
 * API密钥仓库接口
 */
interface APIKeyRepository {
    /**
     * 获取所有API密钥
     */
    fun getAllAPIKeys(): Flow<List<APIKeyInfo>>

    /**
     * 获取指定平台的API密钥
     */
    fun getAPIKeysByPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>>

    /**
     * 获取活跃的API密钥
     */
    fun getActiveAPIKey(): Flow<APIKeyInfo?>

    /**
     * 添加新的API密钥
     */
    suspend fun addAPIKey(info: APIKeyInfo)

    /**
     * 更新API密钥
     */
    suspend fun updateAPIKey(info: APIKeyInfo)

    /**
     * 删除API密钥
     */
    suspend fun deleteAPIKey(id: String)

    /**
     * 设置活跃的API密钥
     */
    suspend fun setActiveAPIKey(id: String)

    /**
     * 获取加密的API密钥内容
     */
    suspend fun getDecryptedAPIKey(id: String): String?
}

/**
 * 对话会话仓库接口
 */
interface ChatSessionRepository {
    /**
     * 获取所有会话
     */
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * 获取指定会话
     */
    suspend fun getSession(id: String): ChatSession?

    /**
     * 创建新会话
     */
    suspend fun createSession(session: ChatSession): String

    /**
     * 更新会话
     */
    suspend fun updateSession(session: ChatSession)

    /**
     * 删除会话
     */
    suspend fun deleteSession(id: String)

    /**
     * 添加消息到会话
     */
    suspend fun addMessageToSession(sessionId: String, message: ChatMessage)

    /**
     * 清空所有会话
     */
    suspend fun clearAllSessions()
}

/**
 * 应用设置仓库接口
 */
interface SettingsRepository {
    /**
     * 获取应用设置
     */
    fun getSettings(): Flow<AppSettings>

    /**
     * 更新应用设置
     */
    suspend fun updateSettings(settings: AppSettings)

    /**
     * 切换暗色模式
     */
    suspend fun toggleDarkMode()

    /**
     * 设置默认平台
     */
    suspend fun setDefaultPlatform(platform: AIPlatform)
}