package com.aichathub.data.remote

import com.aichathub.data.model.AnthropicRequest
import com.aichathub.data.model.AnthropicResponse
import com.aichathub.data.model.GeminiRequest
import com.aichathub.data.model.GeminiResponse
import com.aichathub.data.model.MiniMaxChatRequest
import com.aichathub.data.model.MiniMaxChatResponse
import com.aichathub.data.model.SimpleChatRequest
import com.aichathub.data.model.OpenAIChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * AI 服务 API 接口
 * 所有方法均使用 @Url 接收完整地址，便于支持自定义端点
 */
interface AIServiceApi {

    // ============ OpenAI 兼容端点 ============
    // DeepSeek/Qwen/Zhipu/Moonshot/Yi/Baichuan/Doubao/Hunyuan/Spark/SiliconFlow/Groq/Together/OpenRouter/Custom

    /** 标准鉴权（Authorization: Bearer xxx）的 OpenAI 兼容调用 */
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: SimpleChatRequest
    ): Response<OpenAIChatResponse>

    /** 自定义 Header 的 OpenAI 兼容调用（用于非标准鉴权头，例如自定义平台） */
    @POST
    suspend fun chatCompletionWithHeaders(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: SimpleChatRequest
    ): Response<OpenAIChatResponse>

    // ============ Anthropic Claude ============

    @POST
    suspend fun anthropicMessages(
        @Url url: String,
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): Response<AnthropicResponse>

    // ============ Gemini ============

    @POST
    suspend fun geminiGenerateContent(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    // ============ MiniMax ============

    @POST
    suspend fun miniMaxChat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: MiniMaxChatRequest
    ): Response<MiniMaxChatResponse>

    @POST
    suspend fun miniMaxVLM(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: MiniMaxVLMRequest
    ): Response<MiniMaxVLMResponse>
}

// ============ MiniMax VLM 内联 DTO ============

@kotlinx.serialization.Serializable
data class MiniMaxVLMRequest(
    val model: String = "mmcl-vlm",
    val prompt: String,
    @kotlinx.serialization.SerialName("image_url")
    val imageUrl: String
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMResponse(
    val id: String? = null,
    @kotlinx.serialization.SerialName("base_resp")
    val baseResp: MiniMaxBaseResp? = null,
    val choices: List<MiniMaxVLMChoice> = emptyList()
)

@kotlinx.serialization.Serializable
data class MiniMaxBaseResp(
    @kotlinx.serialization.SerialName("status_code")
    val statusCode: Int = 0,
    @kotlinx.serialization.SerialName("status_msg")
    val statusMsg: String? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMChoice(
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String? = null,
    val index: Int = 0,
    val message: MiniMaxVLMMessage? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMMessage(
    val content: String? = null,
    val role: String = "assistant"
)
