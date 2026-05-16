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
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

// DataStore扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_chat_hub_prefs")

/**
 * API密钥安全存储管理器
 * 使用EncryptedSharedPreferences加密存储敏感信息
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

    // 使用DataStore存储API密钥信息（不含实际密钥）
    private val dataStore = context.dataStore

    companion object {
        private val API_KEYS_KEY = stringPreferencesKey("api_keys_json")
        private val ACTIVE_KEY_ID_KEY = stringPreferencesKey("active_key_id")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val STREAM_RESPONSE_KEY = booleanPreferencesKey("stream_response")
        private val DEFAULT_PLATFORM_KEY = stringPreferencesKey("default_platform")
        private val DEFAULT_TEMPERATURE_KEY = floatPreferencesKey("default_temperature")
        private val DEFAULT_MAX_TOKENS_KEY = intPreferencesKey("default_max_tokens")
        private val CHAT_SESSIONS_KEY = stringPreferencesKey("chat_sessions_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ==================== API密钥管理 ====================

    /**
     * 加密存储API密钥
     */
    fun saveEncryptedKey(id: String, apiKey: String) {
        encryptedPrefs.edit().putString(id, apiKey).apply()
    }

    /**
     * 获取解密的API密钥
     */
    fun getDecryptedKey(id: String): String? {
        return encryptedPrefs.getString(id, null)
    }

    /**
     * 删除加密的API密钥
     */
    fun deleteEncryptedKey(id: String) {
        encryptedPrefs.edit().remove(id).apply()
    }

    /**
     * 获取所有API密钥信息列表
     */
    fun getAllAPIKeys(): Flow<List<APIKeyInfo>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[API_KEYS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<APIKeyInfo>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存API密钥信息列表
     */
    suspend fun saveAPIKeys(keys: List<APIKeyInfo>) {
        dataStore.edit { prefs ->
            prefs[API_KEYS_KEY] = json.encodeToString(keys)
        }
    }

    /**
     * 获取活跃密钥ID
     */
    fun getActiveKeyId(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[ACTIVE_KEY_ID_KEY]
    }

    /**
     * 设置活跃密钥ID
     */
    suspend fun setActiveKeyId(id: String?) {
        dataStore.edit { prefs ->
            if (id != null) {
                prefs[ACTIVE_KEY_ID_KEY] = id
            } else {
                prefs.remove(ACTIVE_KEY_ID_KEY)
            }
        }
    }

    // ==================== 应用设置管理 ====================

    /**
     * 获取应用设置
     */
    fun getSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            isDarkMode = prefs[DARK_MODE_KEY] ?: false,
            enableStreamResponse = prefs[STREAM_RESPONSE_KEY] ?: true,
            defaultPlatform = prefs[DEFAULT_PLATFORM_KEY]?.let {
                try {
                    AIPlatform.valueOf(it)
                } catch (e: Exception) {
                    AIPlatform.DEEPSEEK
                }
            } ?: AIPlatform.DEEPSEEK,
            defaultTemperature = prefs[DEFAULT_TEMPERATURE_KEY] ?: 0.7f,
            defaultMaxTokens = prefs[DEFAULT_MAX_TOKENS_KEY] ?: 2048
        )
    }

    /**
     * 保存应用设置
     */
    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = settings.isDarkMode
            prefs[STREAM_RESPONSE_KEY] = settings.enableStreamResponse
            prefs[DEFAULT_PLATFORM_KEY] = settings.defaultPlatform.name
            prefs[DEFAULT_TEMPERATURE_KEY] = settings.defaultTemperature
            prefs[DEFAULT_MAX_TOKENS_KEY] = settings.defaultMaxTokens
        }
    }

    // ==================== 对话会话管理 ====================

    /**
     * 获取所有对话会话
     */
    fun getAllSessions(): Flow<List<ChatSession>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[CHAT_SESSIONS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<ChatSession>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存所有对话会话
     */
    suspend fun saveSessions(sessions: List<ChatSession>) {
        dataStore.edit { prefs ->
            prefs[CHAT_SESSIONS_KEY] = json.encodeToString(sessions)
        }
    }

    /**
     * 清空所有对话会话
     */
    suspend fun clearSessions() {
        dataStore.edit { prefs ->
            prefs[CHAT_SESSIONS_KEY] = "[]"
        }
    }
}