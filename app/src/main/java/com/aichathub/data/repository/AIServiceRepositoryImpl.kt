package com.aichathub.data.repository

import com.aichathub.data.local.SecureKeyStorage
import com.aichathub.data.model.*
import com.aichathub.data.remote.AIServiceApi
import com.aichathub.data.remote.MiniMaxVLMRequest
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
                    // 使用Map格式进行测试连接
                    val requestMap = mapOf(
                        "model" to model,
                        "messages" to listOf(mapOf(
                            "role" to "user",
                            "content" to "Hi"
                        )),
                        "max_tokens" to 5
                    )
                    val response = api.chatCompletion(
                        url = endpoint,
                        authorization = "Bearer $apiKey",
                        request = requestMap
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
        // DeepSeek使用与OpenAI相同的API格式，复用buildOpenAIRequestMap
        val requestMap = buildOpenAIRequestMap(model, messages, temperature, maxTokens)

        val response = api.chatCompletion(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = requestMap
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
        // 构建请求Map
        val requestMap = buildOpenAIRequestMap(model, messages, temperature, maxTokens)

        val response = api.chatCompletion(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = requestMap
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

    /**
     * 构建OpenAI/DeepSeek 兼容的请求Map
     * 支持纯文本消息和多模态消息（vision API）
     */
    private fun buildOpenAIRequestMap(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): Map<String, Any> {
        val messagesList = messages.map { chatMessage ->
            if (chatMessage.attachments.isEmpty()) {
                // 纯文本消息
                mapOf(
                    "role" to chatMessage.role.name.lowercase(),
                    "content" to chatMessage.content
                )
            } else {
                // 多模态消息：包含附件
                val contentItems = mutableListOf<Map<String, Any>>()

                // 添加文本内容（即使是空字符串也要添加文本项）
                contentItems.add(mapOf(
                    "type" to "text",
                    "text" to (chatMessage.content.ifBlank { "请分析这个文件内容" })
                ))

                // 添加附件 - 详细处理每种类型
                chatMessage.attachments.forEach { attachment ->
                    when (attachment.type) {
                        AttachmentType.IMAGE -> {
                            // 优先使用base64Data
                            val imageData = if (!attachment.base64Data.isNullOrBlank()) {
                                "data:${attachment.mimeType};base64,${attachment.base64Data}"
                            } else if (!attachment.localPath.isNullOrBlank()) {
                                "file://${attachment.localPath}"
                            } else if (!attachment.url.isNullOrBlank()) {
                                attachment.url
                            } else {
                                null
                            }
                            
                            if (imageData != null) {
                                contentItems.add(mapOf(
                                    "type" to "image_url",
                                    "image_url" to mapOf("url" to imageData, "detail" to "auto")
                                ))
                            } else {
                                // 如果没有图片数据，添加文本说明
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[图片文件: ${attachment.fileName}]"
                                ))
                            }
                        }
                        // PDF和文档类型以文本形式提及文件名
                        AttachmentType.PDF -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[PDF文档: ${attachment.fileName}]"
                            ))
                        }
                        AttachmentType.DOCUMENT -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[文档: ${attachment.fileName}]"
                            ))
                        }
                        AttachmentType.ARCHIVE -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[压缩包: ${attachment.fileName}]"
                            ))
                        }
                        else -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[附件: ${attachment.fileName}]"
                            ))
                        }
                    }
                }

                mapOf(
                    "role" to chatMessage.role.name.lowercase(),
                    "content" to contentItems
                )
            }
        }

        return mapOf(
            "model" to model,
            "messages" to messagesList,
            "temperature" to temperature,
            "max_tokens" to maxTokens
        )
    }

    private suspend fun sendMiniMaxMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        // 检查是否有图片附件，如果有则使用VLM端点
        val hasImageAttachments = messages.any { msg ->
            msg.attachments.any { it.type == AttachmentType.IMAGE }
        }

        if (hasImageAttachments) {
            // 使用MiniMax VLM端点处理图片
            return sendMiniMaxVLMMessage(apiKey, model, messages)
        }

        // 没有图片附件，使用标准文本API
        val miniMaxMessages = messages.map { chatMessage ->
            MessageDto(chatMessage.role.name.lowercase(), chatMessage.content)
        }

        val request = MiniMaxChatRequest(
            model = model,
            messages = miniMaxMessages,
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
            
            // 安全地获取内容
            val content = try {
                // 尝试从 messages 格式获取内容
                val contentFromMessages = body.choices?.firstOrNull()?.messages?.lastOrNull()?.content
                // 尝试从 message 格式获取内容
                val contentFromMessage = body.choices?.firstOrNull()?.message?.content
                contentFromMessages ?: contentFromMessage ?: ""
            } catch (e: Exception) {
                // 如果解析失败，返回空字符串
                ""
            }
            
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.MINIMAX,
                model = model,
                usage = body.usage?.let {
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        } else {
            // 尝试读取错误信息
            val errorBody = response.errorBody()?.string() ?: ""
            throw Exception("API Error ${response.code()}: $errorBody")
        }
    }

    /**
     * 使用MiniMax VLM端点发送图片消息
     */
    private suspend fun sendMiniMaxVLMMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): SendMessageResponse {
        // 获取最后一条用户消息及其附件
        val userMessage = messages.filter { it.role == MessageRole.USER }.lastOrNull()
            ?: throw Exception("没有找到用户消息")

        val prompt = userMessage.content.ifBlank { "请分析这张图片" }

        // 构建图片数据
        val imageAttachments = userMessage.attachments.filter { it.type == AttachmentType.IMAGE }
        val imageData = imageAttachments.firstOrNull()?.base64Data
            ?: throw Exception("没有找到图片数据")

        // 构建VLM请求 - 使用MiniMaxVLMRequest数据类
        val vlmRequest = MiniMaxVLMRequest(
            prompt = prompt,
            imageUrl = "data:${userMessage.attachments.first().mimeType};base64,$imageData"
        )

        val response = api.miniMaxVLM(
            url = "https://api.minimax.chat/v1/coding_plan/vlm",
            authorization = "Bearer $apiKey",
            request = vlmRequest
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            val content = body.choices?.firstOrNull()?.message?.content ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.MINIMAX,
                model = model
            )
        } else {
            val errorBody = response.errorBody()?.string() ?: ""
            throw Exception("VLM API Error ${response.code()}: $errorBody")
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
                val parts = mutableListOf<PartDto>()

                // 添加文本内容
                if (msg.content.isNotBlank()) {
                    parts.add(PartDto(text = msg.content))
                }

                // 处理图片附件
                msg.attachments.forEach { attachment ->
                    when (attachment.type) {
                        AttachmentType.IMAGE -> {
                            val base64Data = when {
                                !attachment.base64Data.isNullOrBlank() -> attachment.base64Data
                                else -> null
                            }
                            base64Data?.let {
                                parts.add(PartDto(
                                    inlineData = InlineDataDto(
                                        mimeType = attachment.mimeType,
                                        data = it
                                    )
                                ))
                            }
                        }
                        // 其他类型附件以文本形式提及
                        else -> {
                            parts.add(PartDto(
                                text = "[附件: ${attachment.fileName}]"
                            ))
                        }
                    }
                }

                ContentDto(parts = parts)
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