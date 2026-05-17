package com.aichathub.domain.model

import kotlinx.serialization.Serializable

/**
 * AI平台枚举
 */
@Serializable
enum class AIPlatform(
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val models: List<String> = emptyList()
) {
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/v1/chat/completions",
        defaultModel = "deepseek-chat",
        models = listOf(
            "deepseek-chat",
            "deepseek-coder",
            "deepseek-reasoner"
        )
    ),
    MINIMAX(
        displayName = "MiniMax",
        defaultEndpoint = "https://api.minimax.chat/v1/text/chatcompletion_v2",
        defaultModel = "MiniMax-M2.7",
        models = listOf(
            "MiniMax-M2",
            "MiniMax-M2.1",
            "MiniMax-M2.1-highspeed",
            "MiniMax-M2.5",
            "MiniMax-M2.5-highspeed",
            "MiniMax-M2.7",
            "MiniMax-M2.7-highspeed"
        )
    ),
    OPENAI(
        displayName = "OpenAI (GPT)",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        models = listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4-turbo",
            "gpt-3.5-turbo"
        )
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models",
        defaultModel = "gemini-2.0-flash",
        models = listOf(
            "gemini-2.0-flash",
            "gemini-2.0-flash-exp",
            "gemini-1.5-flash",
            "gemini-1.5-flash-002",
            "gemini-1.5-pro",
            "gemini-1.5-pro-002"
        )
    )
}

/**
 * API密钥信息（支持自定义端点）
 */
@Serializable
data class APIKeyInfo(
    val id: String,
    val platform: AIPlatform,
    val apiKey: String,
    val name: String,
    val customEndpoint: String? = null,  // 自定义API端点
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEndpoint(): String = customEndpoint ?: platform.defaultEndpoint
}

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
 * 附件类型
 */
@Serializable
enum class AttachmentType {
    IMAGE,
    PDF,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

/**
 * 消息附件
 */
@Serializable
data class MessageAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val size: Long = 0,
    val type: AttachmentType = AttachmentType.OTHER,
    val localPath: String? = null,
    val base64Data: String? = null,
    val url: String? = null
)

/**
 * 对话消息（支持多模态）
 */
@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val platform: AIPlatform? = null,
    val model: String? = null,
    val attachments: List<MessageAttachment> = emptyList()
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
    val maxTokens: Int = 2048,
    val endpoint: String? = null
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