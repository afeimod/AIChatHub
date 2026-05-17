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
        // DeepSeek模型支持vision功能（当发送图片时）
        val requestMap = buildOpenAIRequestMap(model, messages, temperature, maxTokens, supportsVision = true)

        val response = api.chatCompletion(
            url = endpoint,
            authorization = "Bearer $apiKey",
            request = requestMap
        )

        if (response.isSuccessful) {
            val body = response.body()!!
            // 安全地获取内容，处理可能的null情况
            val content = body.choices.firstOrNull()?.message?.content?.trim() ?: ""
            return SendMessageResponse(
                content = content,
                platform = AIPlatform.DEEPSEEK,
                model = model,
                usage = body.usage?.let {
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        } else {
            val errorBody = response.errorBody()?.string() ?: ""
            throw Exception("API Error: ${response.code()} - $errorBody")
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
        // OpenAI GPT-4o系列支持vision功能
        val requestMap = buildOpenAIRequestMap(model, messages, temperature, maxTokens, supportsVision = true)

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
        maxTokens: Int,
        supportsVision: Boolean = false
    ): Map<String, Any> {
        // 检测模型是否支持vision（GPT-4o, GPT-4V等支持多模态）
        val modelSupportsVision = supportsVision || model.contains("gpt-4o") || model.contains("vision") || model.contains("4o-mini")
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
                                // 直接使用localPath，支持content://和file://两种格式
                                attachment.localPath
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
                        // PDF和文档类型尝试读取内容
                        AttachmentType.PDF -> {
                            // 尝试使用base64数据作为图片发送（PDF可能不被支持）
                            val pdfData = if (!attachment.base64Data.isNullOrBlank()) {
                                "data:${attachment.mimeType};base64,${attachment.base64Data}"
                            } else {
                                null
                            }
                            
                            if (pdfData != null && modelSupportsVision) {
                                contentItems.add(mapOf(
                                    "type" to "image_url",
                                    "image_url" to mapOf("url" to pdfData, "detail" to "auto")
                                ))
                            } else {
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[PDF文档: ${attachment.fileName}，请分析内容]"
                                ))
                            }
                        }
                        AttachmentType.DOCUMENT -> {
                            // 尝试解码base64获取文本文档内容
                            val textContent = try {
                                if (!attachment.base64Data.isNullOrBlank()) {
                                    val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                                    String(bytes, Charsets.UTF_8)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                            
                            if (textContent != null && textContent.isNotBlank()) {
                                // 文档内容太长时截断
                                val truncatedContent = if (textContent.length > 4000) {
                                    textContent.substring(0, 4000) + "\n...（内容已截断）"
                                } else {
                                    textContent
                                }
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[文档内容如下]\n${truncatedContent}"
                                ))
                            } else {
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[文档: ${attachment.fileName}]"
                                ))
                            }
                        }
                        AttachmentType.ARCHIVE -> {
                            // 压缩包发送base64数据供AI分析
                            val archiveData = if (!attachment.base64Data.isNullOrBlank()) {
                                "data:${attachment.mimeType};base64,${attachment.base64Data}"
                            } else {
                                null
                            }
                            
                            if (archiveData != null && modelSupportsVision) {
                                contentItems.add(mapOf(
                                    "type" to "image_url",
                                    "image_url" to mapOf("url" to archiveData, "detail" to "low")
                                ))
                            } else {
                                contentItems.add(mapOf(
                                    "type" to "text",
                                    "text" to "[压缩包: ${attachment.fileName}，请分析内容]"
                                ))
                            }
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
        // 获取最后一条用户消息，检查是否有附件
        val lastUserMessage = messages.filter { it.role == MessageRole.USER }.lastOrNull()
        val hasImageInLastMessage = lastUserMessage?.attachments?.any { it.type == AttachmentType.IMAGE } == true
        val hasOtherAttachments = lastUserMessage?.attachments?.any { it.type != AttachmentType.IMAGE } == true

        if (hasImageInLastMessage) {
            // 仅当最后一条用户消息包含图片时，才使用VLM端点
            return sendMiniMaxVLMMessage(apiKey, model, messages)
        }

        // 构建消息，包含附件信息
        val miniMaxMessages = messages.map { chatMessage ->
            val content = buildMessageContent(chatMessage)
            MessageDto(chatMessage.role.name.lowercase(), content)
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
     * 构建消息内容，包含附件信息的文本描述
     */
    private fun buildMessageContent(chatMessage: ChatMessage): String {
        var content = chatMessage.content
        
        // 如果有附件，添加附件信息
        if (chatMessage.attachments.isNotEmpty()) {
            val attachmentDescriptions = chatMessage.attachments.map { attachment ->
                when (attachment.type) {
                    AttachmentType.IMAGE -> "[用户发送了图片: ${attachment.fileName}，请分析这张图片的内容]"
                    AttachmentType.PDF -> {
                        // 尝试解码PDF的base64内容
                        val pdfContent = try {
                            if (!attachment.base64Data.isNullOrBlank()) {
                                val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                                String(bytes, Charsets.UTF_8)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                        if (pdfContent != null && pdfContent.isNotBlank()) {
                            val truncatedContent = if (pdfContent.length > 4000) {
                                pdfContent.substring(0, 4000) + "\n...（内容已截断）"
                            } else {
                                pdfContent
                            }
                            "[用户发送了PDF文档: ${attachment.fileName}，内容如下：\n${truncatedContent}]"
                        } else {
                            "[用户发送了PDF文档: ${attachment.fileName}，请分析内容]"
                        }
                    }
                    AttachmentType.DOCUMENT -> {
                        // 尝试解码文档的base64内容
                        val docContent = try {
                            if (!attachment.base64Data.isNullOrBlank()) {
                                val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                                String(bytes, Charsets.UTF_8)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                        if (docContent != null && docContent.isNotBlank()) {
                            val truncatedContent = if (docContent.length > 4000) {
                                docContent.substring(0, 4000) + "\n...（内容已截断）"
                            } else {
                                docContent
                            }
                            "[用户发送了文档: ${attachment.fileName}，内容如下：\n${truncatedContent}]"
                        } else {
                            "[用户发送了文档: ${attachment.fileName}]"
                        }
                    }
                    AttachmentType.ARCHIVE -> {
                        // 尝试解码压缩包的base64内容
                        val archiveContent = try {
                            if (!attachment.base64Data.isNullOrBlank()) {
                                val bytes = android.util.Base64.decode(attachment.base64Data, android.util.Base64.DEFAULT)
                                String(bytes, Charsets.UTF_8)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                        if (archiveContent != null && archiveContent.isNotBlank()) {
                            val truncatedContent = if (archiveContent.length > 4000) {
                                archiveContent.substring(0, 4000) + "\n...（内容已截断）"
                            } else {
                                archiveContent
                            }
                            "[用户发送了压缩包: ${attachment.fileName}，内容如下：\n${truncatedContent}]"
                        } else {
                            "[用户发送了压缩包: ${attachment.fileName}，请分析内容]"
                        }
                    }
                    AttachmentType.OTHER -> "[用户发送了文件: ${attachment.fileName}]"
                }
            }.joinToString("\n")
            
            if (content.isNotBlank()) {
                content = "$attachmentDescriptions\n\n用户消息: $content"
            } else {
                content = attachmentDescriptions
            }
        }
        
        return content
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
            ?: return SendMessageResponse(
                content = "错误：没有找到用户消息",
                platform = AIPlatform.MINIMAX,
                model = model
            )

        val prompt = userMessage.content.ifBlank { "请分析这张图片" }

        // 获取图片数据
        val imageAttachments = userMessage.attachments.filter { it.type == AttachmentType.IMAGE }
        val firstImage = imageAttachments.firstOrNull()

        if (firstImage == null || firstImage.base64Data.isNullOrBlank()) {
            return SendMessageResponse(
                content = "错误：没有找到图片数据，请确保图片已正确上传",
                platform = AIPlatform.MINIMAX,
                model = model
            )
        }

        // 构建图片数据 - 确保使用正确的格式
        val imageData = firstImage.base64Data
        val mimeType = firstImage.mimeType.ifBlank { "image/jpeg" }

        // MiniMax VLM API 需要一个特殊格式的 prompt，包含 <image> 标记
        val fullPrompt = "用户发送了一张图片: ${firstImage.fileName}\n\n请分析这张图片的内容，详细描述图片中有什么。\n\n用户的问题: ${prompt.ifBlank { "请详细描述这张图片" }}"

        // 构建VLM请求 - image_url 可以是URL或base64数据
        val vlmRequest = MiniMaxVLMRequest(
            prompt = fullPrompt,
            imageUrl = "data:$mimeType;base64,$imageData"
        )

        return try {
            val response = api.miniMaxVLM(
                url = "https://api.minimax.chat/v1/coding_plan/vlm",
                authorization = "Bearer $apiKey",
                request = vlmRequest
            )

            if (response.isSuccessful) {
                val body = response.body()!!
                // 从响应中提取内容
                val content = body.choices?.firstOrNull()?.message?.content?.trim()
                    ?: body.base_resp?.status_msg?.trim()
                    ?: "图片分析完成，但未收到详细回复"
                SendMessageResponse(
                    content = content,
                    platform = AIPlatform.MINIMAX,
                    model = model
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMsg = when {
                    errorBody.contains("quota") || errorBody.contains("限额") ->
                        "MiniMax VLM额度已用尽，请明日再试或使用其他平台"
                    errorBody.contains("unauthorized") || errorBody.contains("权限") ->
                        "MiniMax VLM权限不足，请检查API密钥是否正确"
                    errorBody.contains("invalid") || errorBody.contains("参数") ->
                        "MiniMax VLM不支持此图片格式，请尝试使用DeepSeek或OpenAI平台"
                    else ->
                        "MiniMax VLM暂时不可用（错误码: ${response.code()}），请尝试使用DeepSeek或OpenAI平台解析图片"
                }
                SendMessageResponse(
                    content = errorMsg,
                    platform = AIPlatform.MINIMAX,
                    model = model
                )
            }
        } catch (e: Exception) {
            // VLM调用失败时返回友好的错误消息，而不是抛出异常
            SendMessageResponse(
                content = "MiniMax VLM暂时不可用，请尝试使用DeepSeek或OpenAI平台解析图片。错误信息: ${e.message}",
                platform = AIPlatform.MINIMAX,
                model = model
            )
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