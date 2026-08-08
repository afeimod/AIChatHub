package com.aichathub.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aichathub.data.local.SecureKeyStorage
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API 密钥仓库实现
 */
@Singleton
class APIKeyRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : APIKeyRepository {

    override fun getAllAPIKeys(): Flow<List<APIKeyInfo>> = secureStorage.getAllAPIKeys()

    override fun getAPIKeysByPlatform(platform: AIPlatform): Flow<List<APIKeyInfo>> =
        secureStorage.getAllAPIKeys().map { keys -> keys.filter { it.platform == platform } }

    override fun getActiveAPIKey(): Flow<APIKeyInfo?> = secureStorage.getAllAPIKeys().map { keys ->
        val activeId = secureStorage.getActiveKeyId().first()
        keys.find { it.id == activeId && it.isActive } ?: keys.find { it.isActive }
    }

    override suspend fun addAPIKey(info: APIKeyInfo) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        secureStorage.saveEncryptedKey(info.id, info.apiKey)
        currentKeys.add(info.copy(apiKey = ""))
        secureStorage.saveAPIKeys(currentKeys)
    }

    override suspend fun updateAPIKey(info: APIKeyInfo) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        val index = currentKeys.indexOfFirst { it.id == info.id }
        if (index >= 0) {
            if (info.apiKey.isNotBlank()) {
                secureStorage.saveEncryptedKey(info.id, info.apiKey)
            }
            currentKeys[index] = info.copy(apiKey = "")
            secureStorage.saveAPIKeys(currentKeys)
        }
    }

    override suspend fun deleteAPIKey(id: String) {
        val currentKeys = secureStorage.getAllAPIKeys().first().toMutableList()
        currentKeys.removeAll { it.id == id }
        secureStorage.saveAPIKeys(currentKeys)
        secureStorage.deleteEncryptedKey(id)
        val activeId = secureStorage.getActiveKeyId().first()
        if (activeId == id) secureStorage.setActiveKeyId(null)
    }

    override suspend fun setActiveAPIKey(id: String) {
        val currentKeys = secureStorage.getAllAPIKeys().first().map { key ->
            key.copy(isActive = key.id == id)
        }
        secureStorage.saveAPIKeys(currentKeys)
        secureStorage.setActiveKeyId(id)
    }

    override suspend fun getDecryptedAPIKey(id: String): String? = secureStorage.getDecryptedKey(id)
}

/**
 * 对话会话仓库实现
 */
@Singleton
class ChatSessionRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : ChatSessionRepository {

    override fun getAllSessions(): Flow<List<ChatSession>> = secureStorage.getAllSessions()

    override suspend fun getSession(id: String): ChatSession? =
        secureStorage.getAllSessions().first().find { it.id == id }

    override suspend fun createSession(session: ChatSession): String {
        val currentSessions = secureStorage.getAllSessions().first().toMutableList()
        currentSessions.add(0, session)
        secureStorage.saveSessions(currentSessions)
        return session.id
    }

    override suspend fun updateSession(session: ChatSession) {
        val currentSessions = secureStorage.getAllSessions().first().toMutableList()
        val index = currentSessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            currentSessions[index] = session
        } else {
            currentSessions.add(0, session)
        }
        secureStorage.saveSessions(currentSessions)
    }

    override suspend fun deleteSession(id: String) {
        val currentSessions = secureStorage.getAllSessions().first().toMutableList()
        currentSessions.removeAll { it.id == id }
        secureStorage.saveSessions(currentSessions)
    }

    override suspend fun addMessageToSession(sessionId: String, message: ChatMessage) {
        val session = getSession(sessionId) ?: return
        val updatedSession = session.copy(
            messages = session.messages + message,
            updatedAt = System.currentTimeMillis()
        )
        updateSession(updatedSession)
    }

    override suspend fun updateMessage(sessionId: String, message: ChatMessage) {
        val session = getSession(sessionId) ?: return
        val updatedMessages = session.messages.map { if (it.id == message.id) message else it }
        updateSession(session.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteMessage(sessionId: String, messageId: String) {
        val session = getSession(sessionId) ?: return
        val updatedMessages = session.messages.filterNot { it.id == messageId }
        updateSession(session.copy(messages = updatedMessages, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun clearAllSessions() = secureStorage.clearSessions()
}

/**
 * 应用设置仓库实现
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = secureStorage.getSettings()

    override suspend fun updateSettings(settings: AppSettings) {
        secureStorage.saveSettings(settings)
    }

    override suspend fun toggleDarkMode() {
        val settings = secureStorage.getSettings().first()
        secureStorage.saveSettings(settings.copy(isDarkMode = !settings.isDarkMode))
    }

    override suspend fun setDefaultPlatform(platform: AIPlatform) {
        val settings = secureStorage.getSettings().first()
        secureStorage.saveSettings(settings.copy(defaultPlatform = platform))
    }
}

/**
 * 自定义平台仓库实现
 */
@Singleton
class CustomProviderRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : CustomProviderRepository {

    override fun getAllProviders(): Flow<List<CustomProvider>> = secureStorage.getAllCustomProviders()

    override suspend fun getProvider(id: String): CustomProvider? =
        secureStorage.getAllCustomProviders().first().find { it.id == id }

    override suspend fun addProvider(provider: CustomProvider): String {
        val list = secureStorage.getAllCustomProviders().first().toMutableList()
        list.add(provider)
        secureStorage.saveCustomProviders(list)
        return provider.id
    }

    override suspend fun updateProvider(provider: CustomProvider) {
        val list = secureStorage.getAllCustomProviders().first().toMutableList()
        val idx = list.indexOfFirst { it.id == provider.id }
        if (idx >= 0) list[idx] = provider else list.add(provider)
        secureStorage.saveCustomProviders(list)
    }

    override suspend fun deleteProvider(id: String) {
        val list = secureStorage.getAllCustomProviders().first().toMutableList()
        list.removeAll { it.id == id }
        secureStorage.saveCustomProviders(list)
    }
}

/**
 * 工作目录仓库实现
 */
@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureKeyStorage
) : WorkspaceRepository {

    override fun getSettings(): Flow<WorkspaceSettings> = secureStorage.getWorkspaceSettings()

    override suspend fun updateSettings(settings: WorkspaceSettings) {
        secureStorage.saveWorkspaceSettings(settings)
    }

    private fun resolveRootDir(): File? {
        return if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        } else null
    }

    override suspend fun listFiles(): List<WorkspaceFile> = withContext(Dispatchers.IO) {
        val settings = secureStorage.getWorkspaceSettings().first()

        // 自定义目录优先
        if (!settings.useDefaultDownload && settings.customTreeUri != null) {
            val treeUri = runCatching { Uri.parse(settings.customTreeUri) }.getOrNull() ?: return@withContext emptyList()
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            return@withContext root.listFiles().map { doc ->
                WorkspaceFile(
                    name = doc.name ?: "unknown",
                    path = doc.uri.toString(),
                    size = doc.length(),
                    isDirectory = doc.isDirectory,
                    lastModified = doc.lastModified(),
                    mimeType = doc.type ?: "*/*"
                )
            }
        }

        // 默认 Download 目录
        val root = resolveRootDir() ?: return@withContext emptyList()
        if (!root.exists()) root.mkdirs()
        root.listFiles()?.map { f ->
            WorkspaceFile(
                name = f.name,
                path = f.absolutePath,
                size = f.length(),
                isDirectory = f.isDirectory,
                lastModified = f.lastModified(),
                mimeType = guessMime(f.name)
            )
        } ?: emptyList()
    }

    override suspend fun readFile(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val settings = secureStorage.getWorkspaceSettings().first()
        if (!settings.useDefaultDownload && settings.customTreeUri != null) {
            val treeUri = runCatching { Uri.parse(settings.customTreeUri) }.getOrNull() ?: return@withContext null
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
            val file = root.findFile(fileName) ?: return@withContext null
            return@withContext context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
        }
        val root = resolveRootDir() ?: return@withContext null
        val file = File(root, fileName)
        if (file.exists()) file.readBytes() else null
    }

    override suspend fun writeFile(fileName: String, content: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val settings = secureStorage.getWorkspaceSettings().first()
        if (!settings.useDefaultDownload && settings.customTreeUri != null) {
            val treeUri = runCatching { Uri.parse(settings.customTreeUri) }.getOrNull() ?: return@withContext false
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            var file = root.findFile(fileName)
            if (file == null) file = root.createFile("*/*", fileName)
            file ?: return@withContext false
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(content) }
            return@withContext true
        }
        val root = resolveRootDir() ?: return@withContext false
        if (!root.exists()) root.mkdirs()
        File(root, fileName).writeBytes(content)
        true
    }

    override suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val settings = secureStorage.getWorkspaceSettings().first()
        if (!settings.useDefaultDownload && settings.customTreeUri != null) {
            val treeUri = runCatching { Uri.parse(settings.customTreeUri) }.getOrNull() ?: return@withContext false
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            val file = root.findFile(fileName) ?: return@withContext false
            file.delete()
        } else {
            val root = resolveRootDir() ?: return@withContext false
            File(root, fileName).delete()
        }
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "md" -> "text/plain"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "csv" -> "text/csv"
            "xml" -> "application/xml"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
}

/**
 * 终端日志仓库实现
 */
@Singleton
class TerminalLogRepositoryImpl @Inject constructor(
    private val secureStorage: SecureKeyStorage
) : TerminalLogRepository {

    override fun getLogs(): Flow<List<TerminalLog>> = secureStorage.getTerminalLogs()

    override suspend fun addLog(log: TerminalLog) {
        val list = secureStorage.getTerminalLogs().first().toMutableList()
        list.add(log)
        secureStorage.saveTerminalLogs(list)
    }

    override suspend fun addLogs(logs: List<TerminalLog>) {
        val list = secureStorage.getTerminalLogs().first().toMutableList()
        list.addAll(logs)
        secureStorage.saveTerminalLogs(list)
    }

    override suspend fun clearLogs() = secureStorage.clearTerminalLogs()
}
