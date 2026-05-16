package com.aichathub.domain.model

import kotlinx.serialization.Serializable

/**
 * AI平台枚举
 */
@Serializable
enum class AIPlatform(
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String
) {
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/v1/chat/completions",
        defaultModel = "deepseek-chat"
    ),
    MINIMAX(
        displayName = "MiniMax",
        defaultEndpoint = "https://api.minimax.chat/v1/text/chatcompletion_v2",
        defaultModel = "abab6.5s-chat"
    ),
    OPENAI(
        displayName = "OpenAI (GPT)",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-3.5-turbo"
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1/models",
        defaultModel = "gemini-pro"
    )
}

/**
 * API密钥信息
 */
@Serializable
data class APIKeyInfo(
    val id: String,
    val platform: AIPlatform,
    val apiKey: String,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 模型配置
 */
@Serializable
data class ModelConfig(
    val platform: AIPlatform,
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 1.0f
)

/**
 * 对话消息
 */
@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val platform: AIPlatform? = null,
    val model: String? = null
)

/**
 * 消息角色
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * 对话会话
 */
@Serializable
data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "新对话",
    val messages: List<ChatMessage> = emptyList(),
    val platform: AIPlatform = AIPlatform.DEEPSEEK,
    val model: String = "deepseek-chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 发送消息请求
 */
@Serializable
data class SendMessageRequest(
    val platform: AIPlatform,
    val apiKey: String,
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048
)

/**
 * 发送消息响应
 */
@Serializable
data class SendMessageResponse(
    val content: String,
    val platform: AIPlatform,
    val model: String,
    val usage: TokenUsage? = null
)

/**
 * Token使用量
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * 应用设置
 */
@Serializable
data class AppSettings(
    val isDarkMode: Boolean = false,
    val enableStreamResponse: Boolean = true,
    val defaultPlatform: AIPlatform = AIPlatform.DEEPSEEK,
    val defaultTemperature: Float = 0.7f,
    val defaultMaxTokens: Int = 2048
)