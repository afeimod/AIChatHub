package com.aichathub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== OpenAI / DeepSeek API Models ====================

/**
 * 通用的聊天请求格式
 * 支持纯文本消息和多模态消息（vision API）
 */
@Serializable
data class FlexibleChatRequest(
    val model: String,
    val messages: List<FlexibleMessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    val stream: Boolean = false
)

/**
 * 灵活的OpenAI消息格式
 * 支持纯文本消息（content为字符串）和多模态消息（content为对象列表）
 */
@Serializable
data class FlexibleMessageDto(
    val role: String,
    val content: String
)

/**
 * 简化版的聊天请求，用于纯文本消息
 */
@Serializable
data class SimpleChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

/**
 * 多模态消息内容项（用于OpenAI兼容API）
 */
@Serializable
data class MultimodalContentItem(
    val type: String,  // "text" or "image_url"
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: ImageUrlDto? = null
)

@Serializable
data class ImageUrlDto(
    val url: String,
    val detail: String? = "auto"
)

/**
 * 支持多模态的OpenAI消息格式（content为列表）
 */
@Serializable
data class MultimodalMessageDto(
    val role: String,
    val content: List<MultimodalContentItem>
)

/**
 * 多模态聊天请求（支持vision API）
 */
@Serializable
data class MultimodalOpenAIChatRequest(
    val model: String,
    val messages: List<MultimodalMessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    val stream: Boolean = false
)

/**
 * 灵活的消息内容：可以是字符串（纯文本）或内容项列表（多模态）
 */
@Serializable
sealed class FlexibleContent {
    @Serializable
    data class TextContent(val text: String) : FlexibleContent()

    @Serializable
    data class MultimodalContent(val items: List<MultimodalContentItem>) : FlexibleContent()
}

@Serializable
data class OpenAIChatResponse(
    val id: String? = null,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null,
    val model: String? = null,
    @SerialName("created")
    val created: Long? = null
)

@Serializable
data class ChoiceDto(
    val index: Int,
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

// ==================== Gemini API Models ====================

@Serializable
data class GeminiRequest(
    val contents: List<ContentDto>,
    val generationConfig: GenerationConfigDto? = null
)

@Serializable
data class ContentDto(
    val parts: List<PartDto>
)

@Serializable
data class PartDto(
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: InlineDataDto? = null
)

/**
 * Gemini内联数据（用于图片等多模态内容）
 */
@Serializable
data class InlineDataDto(
    val mimeType: String,
    val data: String  // Base64编码
)

@Serializable
data class GenerationConfigDto(
    val temperature: Float? = null,
    @SerialName("maxOutputTokens")
    val maxOutputTokens: Int? = null,
    @SerialName("topP")
    val topP: Float? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<CandidateDto>? = null,
    @SerialName("promptFeedback")
    val promptFeedback: PromptFeedbackDto? = null
)

@Serializable
data class CandidateDto(
    val content: ContentDto? = null,
    @SerialName("finishReason")
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRatingDto>? = null
)

@Serializable
data class PromptFeedbackDto(
    @SerialName("blockReason")
    val blockReason: String? = null,
    val safetyRatings: List<SafetyRatingDto>? = null
)

@Serializable
data class SafetyRatingDto(
    val category: String? = null,
    val probability: String? = null
)

// ==================== MiniMax API Models ====================

@Serializable
data class MiniMaxChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class MiniMaxChatResponse(
    val id: String? = null,
    val choices: List<MiniMaxChoiceDto>,
    val usage: UsageDto? = null,
    val model: String? = null,
    val objectStr: String? = null,
    val created: Long? = null
)

@Serializable
data class MiniMaxChoiceDto(
    val index: Int = 0,
    val messages: List<MessageDto>? = null,
    val message: MessageDto? = null,  // 支持OpenAI格式
    @SerialName("finish_reason")
    val finishReason: String? = null
)

// ==================== Error Response ====================

@Serializable
data class ErrorResponse(
    val error: ErrorDetailDto? = null,
    val message: String? = null,
    val code: String? = null
)

@Serializable
data class ErrorDetailDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
    val param: String? = null
)