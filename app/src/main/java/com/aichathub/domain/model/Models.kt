package com.aichathub.domain.model

import kotlinx.serialization.Serializable

/**
 * AI平台枚举 — 内置支持的平台
 */
@Serializable
enum class AIPlatform(
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val models: List<String> = emptyList(),
    val apiStyle: ApiStyle = ApiStyle.OPENAI,
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer ",
    val website: String = ""
) {
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/v1/chat/completions",
        defaultModel = "deepseek-chat",
        models = listOf(
            "deepseek-chat",
            "deepseek-reasoner",
            "deepseek-coder"
        ),
        website = "https://platform.deepseek.com"
    ),
    OPENAI(
        displayName = "OpenAI (GPT)",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        models = listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4-turbo",
            "gpt-4",
            "gpt-3.5-turbo",
            "o1-mini",
            "o1-preview"
        ),
        website = "https://platform.openai.com"
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
        ),
        apiStyle = ApiStyle.GEMINI,
        authHeader = "x-goog-api-key",
        authPrefix = "",
        website = "https://ai.google.dev"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        defaultEndpoint = "https://api.anthropic.com/v1/messages",
        defaultModel = "claude-3-5-sonnet-20241022",
        models = listOf(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
        ),
        apiStyle = ApiStyle.ANTHROPIC,
        authHeader = "x-api-key",
        authPrefix = "",
        website = "https://console.anthropic.com"
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
            "MiniMax-M2.7-highspeed",
            "abab6.5s-chat",
            "abab6.5-chat"
        ),
        website = "https://platform.minimaxi.com"
    ),
    QWEN(
        displayName = "通义千问 (Qwen)",
        defaultEndpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        defaultModel = "qwen-plus",
        models = listOf(
            "qwen-max",
            "qwen-plus",
            "qwen-turbo",
            "qwen-long",
            "qwen2.5-72b-instruct",
            "qwen2.5-32b-instruct",
            "qwen2.5-14b-instruct",
            "qwen2.5-7b-instruct",
            "qwen2-vl-72b-instruct",
            "qwen2-vl-7b-instruct"
        ),
        website = "https://dashscope.console.aliyun.com"
    ),
    ZHIPU(
        displayName = "智谱 GLM",
        defaultEndpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
        defaultModel = "glm-4-flash",
        models = listOf(
            "glm-4-plus",
            "glm-4-0520",
            "glm-4",
            "glm-4-air",
            "glm-4-flash",
            "glm-4v",
            "glm-4v-plus"
        ),
        website = "https://open.bigmodel.cn"
    ),
    MOONSHOT(
        displayName = "Moonshot (Kimi)",
        defaultEndpoint = "https://api.moonshot.cn/v1/chat/completions",
        defaultModel = "moonshot-v1-8k",
        models = listOf(
            "moonshot-v1-8k",
            "moonshot-v1-32k",
            "moonshot-v1-128k",
            "kimi-latest"
        ),
        website = "https://platform.moonshot.cn"
    ),
    YI(
        displayName = "零一万物 (Yi)",
        defaultEndpoint = "https://api.lingyiwanwu.com/v1/chat/completions",
        defaultModel = "yi-large",
        models = listOf(
            "yi-large",
            "yi-medium",
            "yi-small",
            "yi-vision",
            "yi-large-turbo"
        ),
        website = "https://platform.lingyiwanwu.com"
    ),
    BAICHUAN(
        displayName = "百川 (Baichuan)",
        defaultEndpoint = "https://api.baichuan-ai.com/v1/chat/completions",
        defaultModel = "Baichuan4",
        models = listOf(
            "Baichuan4",
            "Baichuan3-Turbo",
            "Baichuan2-Turbo"
        ),
        website = "https://platform.baichuan-ai.com"
    ),
    DOUBAO(
        displayName = "豆包 (Doubao)",
        defaultEndpoint = "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
        defaultModel = "doubao-pro-32k",
        models = listOf(
            "doubao-pro-32k",
            "doubao-pro-128k",
            "doubao-lite-32k",
            "doubao-lite-128k",
            "doubao-vision-pro-32k"
        ),
        website = "https://www.volcengine.com/product/doubao"
    ),
    HUNYUAN(
        displayName = "腾讯混元",
        defaultEndpoint = "https://api.hunyuan.cloud.tencent.com/v1/chat/completions",
        defaultModel = "hunyuan-pro",
        models = listOf(
            "hunyuan-pro",
            "hunyuan-standard",
            "hunyuan-lite",
            "hunyuan-vision"
        ),
        website = "https://cloud.tencent.com/product/hunyuan"
    ),
    SPARK(
        displayName = "讯飞星火",
        defaultEndpoint = "https://spark-api-open.xf-yun.com/v1/chat/completions",
        defaultModel = "generalv3.5",
        models = listOf(
            "generalv3.5",
            "generalv3",
            "generalv2",
            "general",
            "spark-v4"
        ),
        website = "https://xinghuo.xfyun.cn"
    ),
    SILICONFLOW(
        displayName = "SiliconFlow",
        defaultEndpoint = "https://api.siliconflow.cn/v1/chat/completions",
        defaultModel = "Qwen/Qwen2.5-7B-Instruct",
        models = listOf(
            "Qwen/Qwen2.5-7B-Instruct",
            "Qwen/Qwen2.5-72B-Instruct",
            "deepseek-ai/DeepSeek-V2-Chat",
            "deepseek-ai/DeepSeek-V3",
            "meta-llama/Meta-Llama-3.1-8B-Instruct"
        ),
        website = "https://siliconflow.cn"
    ),
    GROQ(
        displayName = "Groq",
        defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile",
        models = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        ),
        website = "https://console.groq.com"
    ),
    TOGETHER(
        displayName = "Together AI",
        defaultEndpoint = "https://api.together.xyz/v1/chat/completions",
        defaultModel = "meta-llama/Llama-3-8b-chat-hf",
        models = listOf(
            "meta-llama/Llama-3-8b-chat-hf",
            "meta-llama/Llama-3-70b-chat-hf",
            "mistralai/Mixtral-8x7B-Instruct-v0.1",
            "Qwen/Qwen2.5-7B-Instruct-Turbo"
        ),
        website = "https://api.together.xyz"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "openai/gpt-4o-mini",
        models = listOf(
            "openai/gpt-4o-mini",
            "openai/gpt-4o",
            "anthropic/claude-3.5-sonnet",
            "google/gemini-flash-1.5",
            "deepseek/deepseek-chat",
            "qwen/qwen-2.5-72b-instruct"
        ),
        website = "https://openrouter.ai"
    ),
    CUSTOM(
        displayName = "自定义平台",
        defaultEndpoint = "",
        defaultModel = "",
        models = emptyList(),
        website = ""
    );

    companion object {
        /** 内置平台（不含 CUSTOM） */
        val builtIn: List<AIPlatform> = entries.filter { it != CUSTOM }

        /** 根据 ordinal 或名称查找 */
        fun safeValueOf(name: String): AIPlatform =
            runCatching { valueOf(name) }.getOrDefault(CUSTOM)
    }
}

/** API 协议风格 */
@Serializable
enum class ApiStyle {
    /** OpenAI Chat Completions 兼容（绝大多数国产 API） */
    OPENAI,
    /** Anthropic Messages API */
    ANTHROPIC,
    /** Google Gemini generateContent */
    GEMINI
}

/**
 * 自定义平台配置（用户自行添加的 OpenAI 兼容端点）
 */
@Serializable
data class CustomProvider(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val endpoint: String,
    val apiKey: String = "",
    val models: List<String> = emptyList(),
    val defaultModel: String = "",
    val apiStyle: ApiStyle = ApiStyle.OPENAI,
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer ",
    val extraHeaders: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * API密钥信息（支持自定义端点和自定义模型名）
 */
@Serializable
data class APIKeyInfo(
    val id: String,
    val platform: AIPlatform,
    val apiKey: String,
    val name: String,
    val customEndpoint: String? = null,
    val customModels: List<String> = emptyList(),     // 用户自定义可选模型
    val customModelOverride: String? = null,          // 当前指定的自定义模型
    val customProviderId: String? = null,             // 关联的自定义平台 ID
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEndpoint(): String = customEndpoint ?: platform.defaultEndpoint

    /** 该 API Key 可用的模型列表（内置 + 用户自定义） */
    fun availableModels(): List<String> {
        val base = if (platform == AIPlatform.CUSTOM) {
            customModels.ifEmpty { listOf(customModelOverride ?: "") }
        } else {
            platform.models + customModels
        }
        return base.filter { it.isNotBlank() }.distinct()
    }

    /** 默认模型（优先使用 override） */
    fun defaultModel(): String {
        return customModelOverride
            ?: platform.defaultModel.ifBlank { availableModels().firstOrNull() ?: "" }
    }
}

/**
 * 模型配置
 */
@Serializable
data class ModelConfig(
    val platform: AIPlatform,
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val topP: Float = 1.0f
)

/**
 * 附件类型枚举
 */
@Serializable
enum class AttachmentType {
    IMAGE,
    PDF,
    DOCUMENT,
    ARCHIVE,
    AUDIO,
    VIDEO,
    OTHER
}

/**
 * 消息附件数据类
 */
@Serializable
data class MessageAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val type: AttachmentType,
    val localPath: String? = null,
    val base64Data: String? = null,
    val url: String? = null
) {
    companion object {
        fun fromMimeType(mimeType: String, fileName: String, size: Long): MessageAttachment {
            val type = when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType == "application/pdf" -> AttachmentType.PDF
                mimeType.startsWith("audio/") -> AttachmentType.AUDIO
                mimeType.startsWith("video/") -> AttachmentType.VIDEO
                mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z") || mimeType.contains("tar") || mimeType.contains("gz") -> AttachmentType.ARCHIVE
                mimeType.contains("document") || mimeType.contains("text") || mimeType.contains("json") || mimeType.contains("markdown") || mimeType.contains("xml") || mimeType.contains("csv") -> AttachmentType.DOCUMENT
                else -> AttachmentType.OTHER
            }
            return MessageAttachment(
                fileName = fileName,
                mimeType = mimeType,
                size = size,
                type = type
            )
        }
    }
}

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
 * 对话消息（支持多模态附件）
 */
@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val platform: AIPlatform? = null,
    val model: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    val tokenCount: Int = 0,                  // 预估 token 数
    val isStreaming: Boolean = false,         // 是否正在流式输出
    val isError: Boolean = false,             // 是否为错误消息
    val usage: TokenUsage? = null
)

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
    val systemPrompt: String = "",
    val contextStrategy: ContextStrategy = ContextStrategy.UNLIMITED,
    val contextMaxTokens: Int = 128000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 上下文管理策略
 */
@Serializable
enum class ContextStrategy(val displayName: String, val description: String) {
    UNLIMITED("无限制", "发送全部历史消息（受模型限制）"),
    SLIDING_WINDOW("滑动窗口", "保留最近 N 条消息"),
    SUMMARIZE("智能摘要", "压缩旧消息为摘要"),
    SYSTEM_ONLY("仅系统", "只发送系统提示 + 当前消息")
}

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
    val maxTokens: Int = 4096,
    val endpoint: String? = null,
    val systemPrompt: String = "",
    val stream: Boolean = false,
    val customProvider: CustomProvider? = null
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
 * 多模态消息内容项
 */
@Serializable
sealed class ContentItem {
    @Serializable
    data class TextContent(val text: String) : ContentItem()

    @Serializable
    data class ImageContent(
        val url: String? = null,
        val base64: String? = null,
        val detail: String = "low"
    ) : ContentItem()
}

/**
 * 终端日志等级
 */
@Serializable
enum class LogLevel(val displayName: String, val symbol: String) {
    INFO("INFO", "I"),
    REQUEST("REQUEST", "→"),
    RESPONSE("RESPONSE", "←"),
    STREAM("STREAM", "≈"),
    ERROR("ERROR", "✗"),
    WARN("WARN", "!"),
    DEBUG("DEBUG", "D")
}

/**
 * 终端日志条目
 */
@Serializable
data class TerminalLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String = "",
    val message: String,
    val sessionId: String? = null
)

/**
 * 工作目录设置
 */
@Serializable
data class WorkspaceSettings(
    /** 是否使用默认 Download 目录 */
    val useDefaultDownload: Boolean = true,
    /** 自定义工作目录的 SAF tree-uri */
    val customTreeUri: String? = null,
    /** 自定义工作目录显示名 */
    val customDisplayName: String? = null,
    /** 是否自动保存 AI 输出到工作目录 */
    val autoSaveAIResponse: Boolean = false,
    /** 文件名前缀 */
    val filePrefix: String = "aichathub_"
) {
    val displayName: String
        get() = if (useDefaultDownload) "下载目录 (Download)" else (customDisplayName ?: "未设置")
}

/**
 * 工作目录文件
 */
data class WorkspaceFile(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val mimeType: String = "*/*"
)

/**
 * 应用设置（扩展）
 */
@Serializable
data class AppSettings(
    val isDarkMode: Boolean = false,
    val enableStreamResponse: Boolean = true,
    val defaultPlatform: AIPlatform = AIPlatform.DEEPSEEK,
    val defaultTemperature: Float = 0.7f,
    val defaultMaxTokens: Int = 4096,
    val enableMultimodal: Boolean = true,
    val enableMarkdown: Boolean = true,
    val enableTerminalLog: Boolean = true,
    val defaultContextStrategy: ContextStrategy = ContextStrategy.UNLIMITED,
    val defaultContextMaxTokens: Int = 128000,
    val slidingWindowSize: Int = 20,
    val fontSizeScale: Float = 1.0f,
    val enableTokenCounter: Boolean = true,
    val maxHistorySessions: Int = 100,
    val autoTitleFromFirstMessage: Boolean = true
)
