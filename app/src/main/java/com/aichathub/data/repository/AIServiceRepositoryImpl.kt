package com.aichathub.data.repository

import com.aichathub.data.model.*
import com.aichathub.data.remote.AIServiceApi
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.AIServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI服务仓库实现 - 多模态完整支持
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
            val testMessages = listOf(MessageDto("user", "Hi"))

            when (platform) {
                AIPlatform.DEEPSEEK, AIPlatform.OPENAI -> {
                    val requestMap = mapOf(
                        "model" to model,
                        "messages" to listOf(mapOf("role" to "user", "content" to "Hi")),
                        "max_tokens" to 5
                    )
                    val response = api.chatCompletion(
                        url = endpoint,
                        authorization = "Bearer $apiKey",
                        request = requestMap
                    )
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("连接失败: ${response.code()}"))
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
                    else Result.failure(Exception("连接失败: ${response.code()}"))
                }
                AIPlatform.GEMINI -> {
                    val request = GeminiRequest(
                        contents = listOf(ContentDto(parts = listOf(PartDto(text = "Hi")))),
                        generationConfig = GenerationConfigDto(maxOutputTokens = 5)
                    )
                    val response = api.geminiGenerateContent(
                        url = endpoint.trimEnd('/') + "/$model:generateContent",
                        apiKey = apiKey,
                        request = request
                    )
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("连接失败: ${response.code()}"))
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
                usage = body.usage?.let { TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens) }
            )
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API错误 ${response.code()}: $errorBody")
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
                usage = body.usage?.let { TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens) }
            )
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API错误 ${response.code()}: $errorBody")
        }
    }

    /**
     * 构建OpenAI/DeepSeek兼容的请求Map
     * 完整支持多模态：图片、PDF、文档、压缩包等
     */
    private fun buildOpenAIRequestMap(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): Map<String, Any?> {
        val messagesList = messages.map { chatMessage ->
            if (chatMessage.attachments.isEmpty()) {
                // 纯文本消息
                mapOf(
                    "role" to chatMessage.role.name.lowercase(),
                    "content" to chatMessage.content
                )
            } else {
                // 多模态消息处理
                val contentItems = mutableListOf<Map<String, Any>>()

                // 添加文本内容
                val textPrompt = if (chatMessage.content.isNotBlank()) {
                    chatMessage.content
                } else {
                    "请分析这个文件内容"
                }
                contentItems.add(mapOf("type" to "text", "text" to textPrompt))

                // 处理每个附件
                chatMessage.attachments.forEach { attachment ->
                    when (attachment.type) {
                        AttachmentType.IMAGE -> {
                            // 图片：发送base64数据
                            if (!attachment.base64Data.isNullOrBlank()) {
                                contentItems.add(mapOf(
                                    "type" to "image_url",
                                    "image_url" to mapOf(
                                        "url" to "data:${attachment.mimeType};base64,${attachment.base64Data}",
                                        "detail" to "auto"
                                    )
                                ))
                            }
                        }
                        AttachmentType.PDF -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[PDF文档: ${attachment.fileName}](该文件为PDF格式，当前模型无法直接解析PDF内容)"
                            ))
                        }
                        AttachmentType.ARCHIVE -> {
                            contentItems.add(mapOf(
                                "type" to "text",
                                "text" to "[压缩包: ${attachment.fileName}](该文件为压缩包格式，需要解压后才能查看内容)"
                            ))
                        }
                        AttachmentType.DOCUMENT, AttachmentType.OTHER -> {
                            // 尝试解码文本内容
                            val textContent = attachment.base64Data?.let { base64 ->
                                try {
                                    String(Base64.getDecoder().decode(base64))
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (textContent != null && textContent.length < 10000) {
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "【文件: ${attachment.fileName}】\n文件内容如下:\n---\n${textContent}\n---"
                                ))
                            } else {
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[文件: ${attachment.fileName}](文件内容过大或无法解码，请在本地查看)"
                                ))
                            }
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
            "temperature" to temperature.toDouble(),
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
        // 检查是否有图片需要使用VLM端点
        val lastUserMessage = messages.filter { it.role == MessageRole.USER }.lastOrNull()
        val hasImage = lastUserMessage?.attachments?.any { it.type == AttachmentType.IMAGE } == true

        if (hasImage && lastUserMessage != null) {
            return sendMiniMaxVLMMessage(apiKey, endpoint, model, lastUserMessage, messages, temperature, maxTokens)
        }

        // 普通消息（无图片）
        val miniMaxMessages = messages.map { chatMessage ->
            if (chatMessage.attachments.isEmpty()) {
                MessageDto(chatMessage.role.name.lowercase(), chatMessage.content)
            } else {
                // 有附件但无图片
                val contentBuilder = StringBuilder()
                if (chatMessage.content.isNotBlank()) {
                    contentBuilder.appendLine(chatMessage.content)
                }

                chatMessage.attachments.forEach { attachment ->
                    when (attachment.type) {
                        AttachmentType.IMAGE -> {
                            // VLM端点应该已经处理了图片，这里不应该走到这里
                        }
                        AttachmentType.PDF -> {
                            contentBuilder.appendLine("\n[PDF文档: ${attachment.fileName}]")
                        }
                        AttachmentType.DOCUMENT -> {
                            val textContent = attachment.base64Data?.let { base64 ->
                                try { String(Base64.getDecoder().decode(base64)) } catch (e: Exception) { null }
                            }
                            if (textContent != null && textContent.length < 10000) {
                                contentBuilder.appendLine("\n【文件: ${attachment.fileName}】\n${textContent}")
                            } else {
                                contentBuilder.appendLine("\n[文档: ${attachment.fileName}]")
                            }
                        }
                        AttachmentType.ARCHIVE -> {
                            contentBuilder.appendLine("\n[压缩包: ${attachment.fileName}]")
                        }
                        else -> {
                            contentBuilder.appendLine("\n[文件: ${attachment.fileName}]")
                        }
                    }
                }
                MessageDto(chatMessage.role.name.lowercase(), contentBuilder.toString().trim())
            }
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
            val content = body.choices.firstOrNull()?.messages?.lastOrNull()?.content
                ?: body.choices.firstOrNull()?.message?.content
                ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.MINIMAX,
                model = model,
                usage = body.usage?.let { TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens) }
            )
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API错误 ${response.code()}: $errorBody")
        }
    }

    /**
     * MiniMax VLM（视觉语言模型）消息处理
     * 用于处理包含图片的消息
     */
    private suspend fun sendMiniMaxVLMMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        userMessage: ChatMessage,
        historyMessages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): SendMessageResponse {
        // 构建上下文（历史消息）
        val contextBuilder = StringBuilder()
        historyMessages.filter { it.role != MessageRole.USER || it.id != userMessage.id }
            .takeLast(4)
            .forEach { msg ->
                val roleName = if (msg.role == MessageRole.USER) "用户" else "助手"
                contextBuilder.appendLine("$roleName: ${msg.content.take(200)}")
            }

        // 构建当前消息（包含图片）
        val currentMessageBuilder = StringBuilder()
        if (userMessage.content.isNotBlank()) {
            currentMessageBuilder.appendLine(userMessage.content)
        }

        // 获取第一张图片的base64数据
        val firstImage = userMessage.attachments.filter { it.type == AttachmentType.IMAGE }.firstOrNull()
        val imageBase64 = firstImage?.base64Data

        val request = MiniMaxVLMRequest(
            model = model,
            prompt = "上下文:\n${contextBuilder.toString().trim()}\n\n当前消息:\n${currentMessageBuilder.toString().trim()}",
            image_url = if (!imageBase64.isNullOrBlank()) "data:image/jpeg;base64,$imageBase64" else null,
            temperature = temperature,
            max_tokens = maxTokens
        )

        val vlmEndpoint = endpoint.replace("/text/chatcompletion_v2", "/coding_plan/vlm")

        try {
            val response = api.miniMaxVLM(
                url = vlmEndpoint,
                authorization = "Bearer $apiKey",
                request = request
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
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("VLM错误 ${response.code()}: $errorBody")
            }
        } catch (e: Exception) {
            throw Exception("视觉模型调用失败: ${e.message}")
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

                // 处理附件
                msg.attachments.forEach { attachment ->
                    when (attachment.type) {
                        AttachmentType.IMAGE -> {
                            if (!attachment.base64Data.isNullOrBlank()) {
                                parts.add(PartDto(
                                    inlineData = InlineDataDto(
                                        mimeType = attachment.mimeType,
                                        data = attachment.base64Data
                                    )
                                ))
                            }
                        }
                        else -> {
                            val textContent = when (attachment.type) {
                                AttachmentType.PDF -> "[PDF文档: ${attachment.fileName}]"
                                AttachmentType.ARCHIVE -> "[压缩包: ${attachment.fileName}]"
                                else -> {
                                    val decoded = attachment.base64Data?.let { base64 ->
                                        try { String(Base64.getDecoder().decode(base64)) } catch (e: Exception) { null }
                                    }
                                    if (decoded != null && decoded.length < 10000) {
                                        "【文件: ${attachment.fileName}】\n文件内容:\n---\n$decoded\n---"
                                    } else {
                                        "[文件: ${attachment.fileName}]"
                                    }
                                }
                            }
                            parts.add(PartDto(text = textContent))
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
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            throw Exception("API错误 ${response.code()}: $errorBody")
        }
    }
}

/**
 * MiniMax VLM请求
 */
@kotlinx.serialization.Serializable
data class MiniMaxVLMRequest(
    val model: String,
    val prompt: String,
    val image_url: String? = null,
    val temperature: Float? = null,
    @kotlinx.serialization.SerialName("max_tokens")
    val max_tokens: Int? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMResponse(
    val id: String? = null,
    val choices: List<MiniMaxVLMChoice>? = null,
    val usage: UsageDto? = null,
    val model: String? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMChoice(
    val index: Int? = null,
    val message: MiniMaxVLMMessage? = null
)

@kotlinx.serialization.Serializable
data class MiniMaxVLMMessage(
    val content: String? = null
)

@kotlinx.serialization.Serializable
data class InlineDataDto(
    @kotlinx.serialization.SerialName("mime_type")
    val mimeType: String,
    val data: String
)