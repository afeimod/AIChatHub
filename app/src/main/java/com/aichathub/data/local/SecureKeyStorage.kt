package com.aichathub.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aichathub.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_chat_hub_prefs")

/**
 * 安全存储管理器
 * - EncryptedSharedPreferences: 加密存储 API Key 实际内容
 * - DataStore: 存储非敏感元数据、会话、设置、自定义平台、工作目录、终端日志
 */
@Singleton
class SecureKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val dataStore = context.dataStore

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    companion object {
        // API Keys
        private val API_KEYS_KEY = stringPreferencesKey("api_keys_json")
        private val ACTIVE_KEY_ID_KEY = stringPreferencesKey("active_key_id")

        // Settings
        private val SETTINGS_KEY = stringPreferencesKey("app_settings_json")
        // 兼容旧版本的字段
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val STREAM_RESPONSE_KEY = booleanPreferencesKey("stream_response")
        private val DEFAULT_PLATFORM_KEY = stringPreferencesKey("default_platform")
        private val DEFAULT_TEMPERATURE_KEY = floatPreferencesKey("default_temperature")
        private val DEFAULT_MAX_TOKENS_KEY = intPreferencesKey("default_max_tokens")

        // Sessions
        private val CHAT_SESSIONS_KEY = stringPreferencesKey("chat_sessions_json")

        // Custom Providers
        private val CUSTOM_PROVIDERS_KEY = stringPreferencesKey("custom_providers_json")

        // Workspace
        private val WORKSPACE_SETTINGS_KEY = stringPreferencesKey("workspace_settings_json")

        // Terminal logs (only recent N to avoid bloat)
        private val TERMINAL_LOGS_KEY = stringPreferencesKey("terminal_logs_json")
    }

    // ==================== API 密钥管理 ====================

    fun saveEncryptedKey(id: String, apiKey: String) {
        encryptedPrefs.edit().putString(id, apiKey).apply()
    }

    fun getDecryptedKey(id: String): String? = encryptedPrefs.getString(id, null)

    fun deleteEncryptedKey(id: String) {
        encryptedPrefs.edit().remove(id).apply()
    }

    fun getAllAPIKeys(): Flow<List<APIKeyInfo>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[API_KEYS_KEY] ?: "[]"
        try { json.decodeFromString<List<APIKeyInfo>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveAPIKeys(keys: List<APIKeyInfo>) {
        dataStore.edit { prefs -> prefs[API_KEYS_KEY] = json.encodeToString(keys) }
    }

    fun getActiveKeyId(): Flow<String?> = dataStore.data.map { prefs -> prefs[ACTIVE_KEY_ID_KEY] }

    suspend fun setActiveKeyId(id: String?) {
        dataStore.edit { prefs ->
            if (id != null) prefs[ACTIVE_KEY_ID_KEY] = id else prefs.remove(ACTIVE_KEY_ID_KEY)
        }
    }

    // ==================== 应用设置 ====================

    fun getSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        // 优先读取新版统一 JSON
        val jsonStr = prefs[SETTINGS_KEY]
        if (jsonStr != null) {
            try { return@map json.decodeFromString<AppSettings>(jsonStr) } catch (_: Exception) {}
        }
        // 兼容旧版分散存储
        AppSettings(
            isDarkMode = prefs[DARK_MODE_KEY] ?: false,
            enableStreamResponse = prefs[STREAM_RESPONSE_KEY] ?: true,
            defaultPlatform = prefs[DEFAULT_PLATFORM_KEY]?.let {
                runCatching { AIPlatform.valueOf(it) }.getOrDefault(AIPlatform.DEEPSEEK)
            } ?: AIPlatform.DEEPSEEK,
            defaultTemperature = prefs[DEFAULT_TEMPERATURE_KEY] ?: 0.7f,
            defaultMaxTokens = prefs[DEFAULT_MAX_TOKENS_KEY] ?: 4096
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json.encodeToString(settings)
            // 同步旧版字段（双写，避免回滚）
            prefs[DARK_MODE_KEY] = settings.isDarkMode
            prefs[STREAM_RESPONSE_KEY] = settings.enableStreamResponse
            prefs[DEFAULT_PLATFORM_KEY] = settings.defaultPlatform.name
            prefs[DEFAULT_TEMPERATURE_KEY] = settings.defaultTemperature
            prefs[DEFAULT_MAX_TOKENS_KEY] = settings.defaultMaxTokens
        }
    }

    // ==================== 对话会话 ====================

    fun getAllSessions(): Flow<List<ChatSession>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[CHAT_SESSIONS_KEY] ?: "[]"
        try { json.decodeFromString<List<ChatSession>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveSessions(sessions: List<ChatSession>) {
        dataStore.edit { prefs ->
            // 体积保护：保留最近 maxHistorySessions 个会话，且每个会话最多保留最近 100 条消息
            val trimmed = sessions.take(100).map { s ->
                s.copy(messages = s.messages.takeLast(100))
            }
            prefs[CHAT_SESSIONS_KEY] = json.encodeToString(trimmed)
        }
    }

    suspend fun clearSessions() {
        dataStore.edit { prefs -> prefs[CHAT_SESSIONS_KEY] = "[]" }
    }

    // ==================== 自定义平台 ====================

    fun getAllCustomProviders(): Flow<List<CustomProvider>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[CUSTOM_PROVIDERS_KEY] ?: "[]"
        try { json.decodeFromString<List<CustomProvider>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveCustomProviders(providers: List<CustomProvider>) {
        dataStore.edit { prefs -> prefs[CUSTOM_PROVIDERS_KEY] = json.encodeToString(providers) }
    }

    // ==================== 工作目录 ====================

    fun getWorkspaceSettings(): Flow<WorkspaceSettings> = dataStore.data.map { prefs ->
        val jsonStr = prefs[WORKSPACE_SETTINGS_KEY]
        if (jsonStr != null) {
            try { return@map json.decodeFromString<WorkspaceSettings>(jsonStr) } catch (_: Exception) {}
        }
        WorkspaceSettings()
    }

    suspend fun saveWorkspaceSettings(settings: WorkspaceSettings) {
        dataStore.edit { prefs -> prefs[WORKSPACE_SETTINGS_KEY] = json.encodeToString(settings) }
    }

    // ==================== 终端日志 ====================

    fun getTerminalLogs(): Flow<List<TerminalLog>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[TERMINAL_LOGS_KEY] ?: "[]"
        try { json.decodeFromString<List<TerminalLog>>(jsonStr) } catch (e: Exception) { emptyList() }
    }

    suspend fun saveTerminalLogs(logs: List<TerminalLog>) {
        dataStore.edit { prefs ->
            // 只保留最近 500 条，避免 DataStore 膨胀
            val trimmed = logs.takeLast(500)
            prefs[TERMINAL_LOGS_KEY] = json.encodeToString(trimmed)
        }
    }

    suspend fun clearTerminalLogs() {
        dataStore.edit { prefs -> prefs[TERMINAL_LOGS_KEY] = "[]" }
    }
}
