package com.aichathub.data.repository

import com.aichathub.data.local.SecureKeyStorage
import com.aichathub.data.model.*
import com.aichathub.data.remote.AIServiceApi
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.AIServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI服务仓库实现
 */
@Singleton
class AIServiceRepositoryImpl @Inject constructor(
    private val api: AIServiceApi
) : AIServiceRepository {

    override suspend fun sendMessage(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): Result<SendMessageResponse> = withContext(Dispatchers.IO) {
        try {
            val result = when (platform) {
                AIPlatform.DEEPSEEK -> sendDeepSeekMessage(apiKey, endpoint, model, messages, temperature, maxTokens)
                AIPlatform.OPENAI -> sendOpenAIMessage(apiKey, endpoint, model, messages, temperature, maxTokens)
                AIPlatform.MINIMAX -> sendMiniMaxMessage(apiKey, endpoint, model, messages, temperature, maxTokens)
                AIPlatform.GEMINI -> sendGeminiMessage(apiKey, endpoint, model, messages, temperature, maxTokens)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testMessages = listOf(
                MessageDto("user", "Hi")
            )

            when (platform) {
                AIPlatform.DEEPSEEK, AIPlatform.OPENAI -> {
                    val request = OpenAIChatRequest(
                        model = model,
                        messages = testMessages,
                        maxTokens = 5
                    )
                    val response = api.chatCompletion(
                        url = endpoint,
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("Connection failed: ${response.code()}"))
                }
                AIPlatform.MINIMAX -> {
                    val request = MiniMaxChatRequest(
                        model = model,
                        messages = testMessages,
                        maxTokens = 5
                    )
                    val response = api.miniMaxChat(
                        url = endpoint,
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("Connection failed: ${response.code()}"))
                }
                AIPlatform.GEMINI -> {
                    val request = GeminiRequest(
                        contents = listOf(
                            ContentDto(parts = listOf(PartDto(text = "Hi")))
                        ),
                        generationConfig = GenerationConfigDto(maxOutputTokens = 5)
                    )
                    val response = api.geminiGenerateContent(
                        url = endpoint.trimEnd('/') + "/$model:generateContent",
                        apiKey = apiKey,
                        request = request
                    )
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("Connection failed: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendDeepSeekMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        val request = OpenAIChatRequest(
            model = model,
            messages = messages.map { MessageDto(it.role.name.lowercase(), it.content) },
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = api.chatCompletion(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = request
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            val content = body.choices.firstOrNull()?.message?.content ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.DEEPSEEK,
                model = model,
                usage = body.usage?.let {
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        } else {
            throw Exception("API Error: ${response.code()} - ${response.message()}")
        }
    }

    private suspend fun sendOpenAIMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        val request = OpenAIChatRequest(
            model = model,
            messages = messages.map { MessageDto(it.role.name.lowercase(), it.content) },
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = api.chatCompletion(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = request
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            val content = body.choices.firstOrNull()?.message?.content ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.OPENAI,
                model = model,
                usage = body.usage?.let {
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        } else {
            throw Exception("API Error: ${response.code()} - ${response.message()}")
        }
    }

    private suspend fun sendMiniMaxMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        val request = MiniMaxChatRequest(
            model = model,
            messages = messages.map { MessageDto(it.role.name.lowercase(), it.content) },
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = api.miniMaxChat(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = request
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            // 尝试从 messages 格式获取内容（MiniMax自定义格式）
            val contentFromMessages = body.choices.firstOrNull()?.messages?.lastOrNull()?.content
            // 尝试从 message 格式获取内容（OpenAI兼容格式）
            val contentFromMessage = body.choices.firstOrNull()?.message?.content
            val content = contentFromMessages ?: contentFromMessage ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.MINIMAX,
                model = model,
                usage = body.usage?.let {
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        } else {
            throw Exception("API Error: ${response.code()} - ${response.message()}")
        }
    }

    private suspend fun sendGeminiMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        val contents = messages
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .map { msg ->
                ContentDto(
                    parts = listOf(PartDto(text = msg.content))
                )
            }

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GenerationConfigDto(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )

        val response = api.geminiGenerateContent(
            url = "${endpoint.trimEnd('/')}/$model:generateContent",
            apiKey = apiKey,
            request = request
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            val content = body.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.GEMINI,
                model = model
            )
        } else {
            throw Exception("API Error: ${response.code()} - ${response.message()}")
        }
    }
}