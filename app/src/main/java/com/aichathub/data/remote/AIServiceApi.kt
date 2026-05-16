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
     * 支持纯文本和多模态消息（通过FlexibleMessageDto）
     */
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
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
}

/**
 * API响应包装类
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()
}