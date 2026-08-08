package com.aichathub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== OpenAI / DeepSeek API Models ====================

/**
 * 通用的聊天请求格式（带流式支持）
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

@Serializable
data class FlexibleMessageDto(
    val role: String,
    val content: String
)

/**
 * 纯文本聊天请求（OpenAI 兼容 — 同时支持 stream 字段）
 */
@Serializable
data class SimpleChatRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    val stream: Boolean = false
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
    val type: String,
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
 * 多模态聊天请求（vision + stream）
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

@Serializable
data class OpenAIChatResponse(
    val id: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val model: String? = null,
    @SerialName("created")
    val created: Long? = null
)

@Serializable
data class ChoiceDto(
    val index: Int = 0,
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

// ==================== OpenAI SSE Stream Chunk ====================

@Serializable
data class OpenAIStreamChunk(
    val id: String? = null,
    val choices: List<OpenAIStreamChoice> = emptyList()
)

@Serializable
data class OpenAIStreamChoice(
    val index: Int = 0,
    val delta: OpenAIDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null
)

// ==================== Anthropic Claude API Models ====================

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    val system: String? = null,
    val stream: Boolean = false
)

/** Anthropic 消息 — content 为字符串（最简单也最兼容） */
@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val id: String? = null,
    val type: String = "message",
    val role: String = "assistant",
    val content: List<AnthropicContentBlock> = emptyList(),
    val model: String? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    val usage: AnthropicUsage? = null
)

@Serializable
data class AnthropicContentBlock(
    val type: String = "text",
    val text: String? = null
)

@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens")
    val inputTokens: Int = 0,
    @SerialName("output_tokens")
    val outputTokens: Int = 0
)

// Anthropic SSE events
@Serializable
data class AnthropicStreamEvent(
    val type: String,
    val delta: AnthropicDelta? = null
)

@Serializable
data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null
)

// ==================== Gemini API Models ====================

@Serializable
data class GeminiRequest(
    val contents: List<ContentDto>,
    val generationConfig: GenerationConfigDto? = null,
    @SerialName("system_instruction")
    val systemInstruction: ContentDto? = null
)

@Serializable
data class ContentDto(
    val parts: List<PartDto>,
    val role: String? = null
)

@Serializable
data class PartDto(
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: InlineDataDto? = null
)

@Serializable
data class InlineDataDto(
    val mimeType: String,
    val data: String
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
    val promptFeedback: PromptFeedbackDto? = null,
    @SerialName("usageMetadata")
    val usageMetadata: GeminiUsageMetadata? = null
)

@Serializable
data class GeminiUsageMetadata(
    @SerialName("promptTokenCount")
    val promptTokenCount: Int = 0,
    @SerialName("candidatesTokenCount")
    val candidatesTokenCount: Int = 0,
    @SerialName("totalTokenCount")
    val totalTokenCount: Int = 0
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
    val choices: List<MiniMaxChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val model: String? = null,
    @SerialName("object")
    val objectStr: String? = null,
    val created: Long? = null
)

@Serializable
data class MiniMaxChoiceDto(
    val index: Int = 0,
    val messages: List<MessageDto>? = null,
    val message: MessageDto? = null,
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
