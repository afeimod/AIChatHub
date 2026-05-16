package com.aichathub.data.repository

import com.aichathub.data.local.SecureKeyStorage
import com.aichathub.domain.model.APIKeyInfo
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.repository.APIKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API密钥仓库实现
 */
@Singleton
class APIKeyRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : APIKeyRepository {

    override fun getAllAPIKeys(): Flow<List<APIKeyInfo>> {
        return secureStorage.getAllAPIKeys()
    }

    override fun getAPIKeysByPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>> {
        return secureStorage.getAllAPIKeys().map { keys ->
            keys.filter { it.platform == platform }
        }
    }

    override fun getActiveAPIKey(): Flow<APIKeyInfo?> {
        return secureStorage.getAllAPIKeys().map { keys ->
            val activeId = secureStorage.getActiveKeyId().first()
            keys.find { it.id == activeId && it.isActive }
                ?: keys.find { it.isActive }
        }
    }

    override suspend fun addAPIKey(info: APIKeyInfo) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        // 保存加密的API密钥
        secureStorage.saveEncryptedKey(info.id, info.apiKey)
        // 保存密钥信息（不包含实际密钥内容）
        val keyInfoToSave = info.copy(apiKey = "") // 清空敏感信息
        currentKeys.add(keyInfoToSave)
        secureStorage.saveAPIKeys(currentKeys)
    }

    override suspend fun updateAPIKey(info: APIKeyInfo) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        val index = currentKeys.indexOfFirst { it.id == info.id }
        if (index >= 0) {
            // 如果提供了新的API密钥，更新加密存储
            if (info.apiKey.isNotBlank()) {
                secureStorage.saveEncryptedKey(info.id, info.apiKey)
            }
            // 更新密钥信息
            currentKeys[index] = info.copy(apiKey = "")
            secureStorage.saveAPIKeys(currentKeys)
        }
    }

    override suspend fun deleteAPIKey(id: String) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        currentKeys.removeAll { it.id == id }
        secureStorage.saveAPIKeys(currentKeys)
        secureStorage.deleteEncryptedKey(id)

        // 如果删除的是活跃密钥，清除活跃状态
        val activeId = secureStorage.getActiveKeyId().first()
        if (activeId == id) {
            secureStorage.setActiveKeyId(null)
        }
    }

    override suspend fun setActiveAPIKey(id: String) {
        val currentKeys = secureStorage.getAllAPIKeys().first().map { key ->
            key.copy(isActive = key.id == id)
        }
        secureStorage.saveAPIKeys(currentKeys)
        secureStorage.setActiveKeyId(id)
    }

    override suspend fun getDecryptedAPIKey(id: String): String? {
        return secureStorage.getDecryptedKey(id)
    }
}

/**
 * 对话会话仓库实现
 */
@Singleton
class ChatSessionRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : com.aichathub.domain.repository.ChatSessionRepository {

    override fun getAllSessions(): Flow<List<com.aichathub.domain.model.ChatSession>> {
        return secureStorage.getAllSessions()
    }

    override suspend fun getSession(id: String): com.aichathub.domain.model.ChatSession? {
        return secureStorage.getAllSessions().let { flow ->
            var result: com.aichathub.domain.model.ChatSession? = null
            flow.collect { sessions ->
                result = sessions.find { it.id == id }
                return@collect
            }
            result
        }
    }

    override suspend fun createSession(session: com.aichathub.domain.model.ChatSession): String {
        val currentSessions = mutableListOf<com.aichathub.domain.model.ChatSession>()
        secureStorage.getAllSessions().collect { sessions ->
            currentSessions.addAll(sessions)
            return@collect
        }
        currentSessions.add(0, session) // 新会话添加到最前面
        secureStorage.saveSessions(currentSessions)
        return session.id
    }

    override suspend fun updateSession(session: com.aichathub.domain.model.ChatSession) {
        val currentSessions = mutableListOf<com.aichathub.domain.model.ChatSession>()
        secureStorage.getAllSessions().collect { sessions ->
            currentSessions.addAll(sessions)
            return@collect
        }
        val index = currentSessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            currentSessions[index] = session
            secureStorage.saveSessions(currentSessions)
        }
    }

    override suspend fun deleteSession(id: String) {
        val currentSessions = mutableListOf<com.aichathub.domain.model.ChatSession>()
        secureStorage.getAllSessions().collect { sessions ->
            currentSessions.addAll(sessions)
            return@collect
        }
        currentSessions.removeAll { it.id == id }
        secureStorage.saveSessions(currentSessions)
    }

    override suspend fun addMessageToSession(sessionId: String, message: com.aichathub.domain.model.ChatMessage) {
        val session = getSession(sessionId) ?: return
        val updatedMessages = session.messages + message
        val updatedSession = session.copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
        updateSession(updatedSession)
    }

    override suspend fun clearAllSessions() {
        secureStorage.clearSessions()
    }
}

/**
 * 应用设置仓库实现
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : com.aichathub.domain.repository.SettingsRepository {

    override fun getSettings(): Flow<com.aichathub.domain.model.AppSettings> {
        return secureStorage.getSettings()
    }

    override suspend fun updateSettings(settings: com.aichathub.domain.model.AppSettings) {
        secureStorage.saveSettings(settings)
    }

    override suspend fun toggleDarkMode() {
        val currentSettings = mutableMapOf<kotlinx.coroutines.flow.Flow<com.aichathub.domain.model.AppSettings>, com.aichathub.domain.model.AppSettings>()
        secureStorage.getSettings().collect { settings ->
            val newSettings = settings.copy(isDarkMode = !settings.isDarkMode)
            secureStorage.saveSettings(newSettings)
            return@collect
        }
    }

    override suspend fun setDefaultPlatform(platform: AIPlatform) {
        secureStorage.getSettings().collect { settings ->
            val newSettings = settings.copy(defaultPlatform = platform)
            secureStorage.saveSettings(newSettings)
            return@collect
        }
    }
}