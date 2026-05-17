package com.aichathub.data.remote

import com.aichathub.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 统一AI服务API接口
 */
interface AIServiceApi {

    /**
     * OpenAI/DeepSeek 格式的聊天请求
     * 使用FlexibleChatRequest来处理多模态和纯文本消息
     */
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: FlexibleChatRequest
    ): Response<OpenAIChatResponse>

    /**
     * Gemini API 请求
     */
    @POST
    suspend fun geminiGenerateContent(
        @Url url: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    /**
     * MiniMax API 请求
     */
    @POST
    suspend fun miniMaxChat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: MiniMaxChatRequest
    ): Response<MiniMaxChatResponse>

    /**
     * MiniMax VLM (Vision Language Model) 请求
     * 用于处理图片等多模态输入
     */
    @POST
    suspend fun miniMaxVLM(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: MiniMaxVLMRequest
    ): Response<MiniMaxVLMResponse>
}

/**
 * MiniMax VLM 请求模型
 */
@kotlinx.serialization.Serializable
data class MiniMaxVLMRequest(
    val prompt: String,
    @kotlinx.serialization.SerialName("image_url")
    val imageUrl: String
)

/**
 * MiniMax VLM 响应模型
 */
@kotlinx.serialization.Serializable
data class MiniMaxVLMResponse(
    val id: String? = null,
    val choices: List<MiniMaxVLMChoice>? = null,
    val created: Long? = null,
    val base_resp: MiniMaxBaseResp? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxBaseResp(
    val status_code: Int = 0,
    val status_msg: String = ""
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMChoice(
    val index: Int = 0,
    val message: MiniMaxVLMMessage? = null,
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMMessage(
    val role: String = "assistant",
    val content: String = ""
)

/**
 * API响应包装类
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()
}