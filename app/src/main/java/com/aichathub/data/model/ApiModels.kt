package com.aichathub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== OpenAI / DeepSeek API Models ====================

@Serializable
data class OpenAIChatRequest(
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
    val text: String
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
    val objectStr: String? = null
)

@Serializable
data class MiniMaxChoiceDto(
    val index: Int,
    val messages: List<MessageDto>,
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