package com.aichathub.data.repository

import com.aichathub.data.local.TerminalLogManager
import com.aichathub.data.model.*
import com.aichathub.data.remote.AIServiceApi
import com.aichathub.data.remote.MiniMaxVLMRequest
import com.aichathub.domain.model.*
import com.aichathub.domain.repository.AIServiceRepository
import com.aichathub.domain.util.ContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 服务仓库实现 — 统一调度所有平台
 *
 * 平台分组：
 *  - OpenAI 兼容（DEEPSEEK / OPENAI / MINIMAX / QWEN / ZHIPU / MOONSHOT / YI / BAICHUAN / DOUBAO / HUNYUAN / SPARK / SILICONFLOW / GROQ / TOGETHER / OPENROUTER / CUSTOM）
 *  - Anthropic（ANTHROPIC）
 *  - Gemini（GEMINI）
 */
@Singleton
class AIServiceRepositoryImpl @Inject constructor(
    private val api: AIServiceApi,
    private val httpClient: OkHttpClient,
    private val json: kotlinx.serialization.json.Json,
    private val logManager: TerminalLogManager
) : AIServiceRepository {

    companion object {
        private const val TAG = "AIService"
    }

    // ============ 非流式 ============

    override suspend fun sendMessage(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String,
        customProvider: CustomProvider?
    ): Result<SendMessageResponse> = withContext(Dispatchers.IO) {
        try {
            logManager.request(TAG, "→ [${platform.displayName}] model=$model endpoint=$endpoint msgs=${messages.size}")
            val result = when {
                customProvider != null -> sendCustomMessage(customProvider, apiKey, model, messages, temperature, maxTokens, systemPrompt)
                platform.apiStyle == ApiStyle.ANTHROPIC -> sendAnthropicMessage(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt)
                platform.apiStyle == ApiStyle.GEMINI -> sendGeminiMessage(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt)
                platform == AIPlatform.MINIMAX -> sendMiniMaxMessage(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt)
                else -> sendOpenAICompatibleMessage(platform, apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt)
            }
            logManager.response(TAG, "← [${platform.displayName}] success len=${result.content.length}")
            Result.success(result)
        } catch (e: Exception) {
            logManager.error(TAG, "✗ [${platform.displayName}] ${e.message}")
            Result.failure(e)
        }
    }

    // ============ 流式 ============

    override fun sendMessageStream(
        platform: AIPlatform,
        apiKey: String,
        model: String,
        endpoint: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String,
        customProvider: CustomProvider?
    ): Flow<String> = flow {
        logManager.request(TAG, "→> stream [${platform.displayName}] model=$model")
        try {
            val style = customProvider?.apiStyle ?: platform.apiStyle
            when (style) {
                ApiStyle.ANTHROPIC -> streamAnthropic(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt).collect { emit(it) }
                ApiStyle.GEMINI -> streamGemini(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt).collect { emit(it) }
                ApiStyle.OPENAI -> {
                    if (platform == AIPlatform.MINIMAX && customProvider == null) {
                        streamMiniMax(apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt).collect { emit(it) }
                    } else {
                        streamOpenAICompatible(customProvider, platform, apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt).collect { emit(it) }
                    }
                }
            }
            logManager.response(TAG, "←< stream [${platform.displayName}] done")
        } catch (e: Exception) {
            logManager.error(TAG, "✗< stream [${platform.displayName}] ${e.message}")
            throw e
        }
    }

    // ============ 连接测试 ============

    override suspend fun testConnection(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String,
        customProvider: CustomProvider?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testMessages = listOf(ChatMessage(role = MessageRole.USER, content = "Hi"))
            val result = sendMessage(
                platform = platform,
                apiKey = apiKey,
                model = model,
                endpoint = endpoint,
                messages = testMessages,
                temperature = 0.7f,
                maxTokens = 16,
                systemPrompt = "",
                customProvider = customProvider
            )
            Result.success(result.isSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============ OpenAI 兼容（含 DeepSeek / Qwen / Zhipu / Moonshot / Yi / Baichuan / Doubao / Hunyuan / Spark / SiliconFlow / Groq / Together / OpenRouter / MiniMax / CUSTOM） ============

    private suspend fun sendOpenAICompatibleMessage(
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): SendMessageResponse {
        val request = buildOpenAIRequest(model, messages, temperature, maxTokens, systemPrompt, stream = false)
        val response = api.chatCompletion(endpoint, "Bearer $apiKey", request)
        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string() ?: ""
            throw RuntimeException("HTTP ${response.code()}: $errBody".take(500))
        }
        val body = response.body() ?: throw RuntimeException("Empty response body")
        val content = body.choices.firstOrNull()?.message?.content ?: ""
        val usage = body.usage?.let {
            TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
        }
        return SendMessageResponse(content = content, platform = platform, model = model, usage = usage)
    }

    private fun streamOpenAICompatible(
        customProvider: CustomProvider?,
        platform: AIPlatform,
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        val request = buildOpenAIRequest(model, messages, temperature, maxTokens, systemPrompt, stream = true)
        val payload = json.encodeToString(SimpleChatRequest.serializer(), request)
        val reqBuilder = Request.Builder().url(endpoint).post(payload.toRequestBody("application/json".toMediaType()))
        // 鉴权头：自定义平台支持
        val authHeader = customProvider?.authHeader ?: platform.authHeader
        val authPrefix = customProvider?.authPrefix ?: platform.authPrefix
        reqBuilder.header(authHeader, "$authPrefix$apiKey")
        customProvider?.extraHeaders?.forEach { (k, v) -> reqBuilder.header(k, v) }

        executeSse(reqBuilder.build()) { line ->
            parseOpenAIStreamChunk(line)?.let { emit(it) }
        }
    }

    private fun buildOpenAIRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String,
        stream: Boolean
    ): SimpleChatRequest {
        val msgList = mutableListOf<MessageDto>()
        if (systemPrompt.isNotBlank()) {
            msgList.add(MessageDto(role = "system", content = systemPrompt))
        }
        ContextManager.toApiMessages(messages).forEach { m ->
            val content = buildOpenAIContent(m)
            msgList.add(MessageDto(role = mapRole(m.role), content = content))
        }
        return SimpleChatRequest(
            model = model,
            messages = msgList,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = stream
        )
    }

    /** 构造 OpenAI content — 文本 + 图片(若附件为 IMAGE 则使用 data URI 文本嵌入；多数 OpenAI 兼容平台支持) */
    private fun buildOpenAIContent(message: ChatMessage): String {
        if (message.attachments.isEmpty()) return message.content
        val sb = StringBuilder(message.content)
        message.attachments.forEach { att ->
            sb.append("\n\n")
            when (att.type) {
                AttachmentType.IMAGE -> {
                    val dataUri = att.base64Data?.let { "data:${att.mimeType};base64,$it" } ?: att.url ?: ""
                    sb.append("[图片: ${att.fileName}] $dataUri")
                }
                AttachmentType.PDF, AttachmentType.DOCUMENT -> {
                    // 仅附加文件名，避免无效 UTF-8 文本污染上下文
                    sb.append("[附件: ${att.fileName} (${att.mimeType})]")
                }
                else -> sb.append("[附件: ${att.fileName}]")
            }
        }
        return sb.toString()
    }

    private fun parseOpenAIStreamChunk(line: String): String? {
        if (!line.startsWith("data:")) return null
        val data = line.removePrefix("data:").trim()
        if (data == "[DONE]" || data.isEmpty()) return null
        return try {
            val chunk = json.decodeFromString(OpenAIStreamChunk.serializer(), data)
            chunk.choices.firstOrNull()?.delta?.content
        } catch (_: Exception) { null }
    }

    // ============ Anthropic Claude ============

    private suspend fun sendAnthropicMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): SendMessageResponse {
        val request = buildAnthropicRequest(model, messages, temperature, maxTokens, systemPrompt, stream = false)
        val response = api.anthropicMessages(endpoint, apiKey, "2023-06-01", request)
        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string() ?: ""
            throw RuntimeException("HTTP ${response.code()}: $errBody".take(500))
        }
        val body = response.body() ?: throw RuntimeException("Empty Anthropic response")
        val content = body.content.firstOrNull()?.text ?: ""
        val usage = body.usage?.let { TokenUsage(it.inputTokens, it.outputTokens, it.inputTokens + it.outputTokens) }
        return SendMessageResponse(content = content, platform = AIPlatform.ANTHROPIC, model = model, usage = usage)
    }

    private fun streamAnthropic(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        val request = buildAnthropicRequest(model, messages, temperature, maxTokens, systemPrompt, stream = true)
        val payload = json.encodeToString(AnthropicRequest.serializer(), request)
        val req = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()
        executeSse(req) { line ->
            parseAnthropicStreamChunk(line)?.let { emit(it) }
        }
    }

    private fun buildAnthropicRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String,
        stream: Boolean
    ): AnthropicRequest {
        val msgs = ContextManager.toApiMessages(messages).map { m ->
            AnthropicMessage(
                role = if (m.role == MessageRole.USER) "user" else "assistant",
                content = m.content.ifBlank { "(empty)" }
            )
        }
        return AnthropicRequest(
            model = model,
            messages = msgs,
            maxTokens = maxTokens.coerceAtLeast(1),
            temperature = temperature,
            system = systemPrompt.ifBlank { null },
            stream = stream
        )
    }

    private fun parseAnthropicStreamChunk(line: String): String? {
        if (!line.startsWith("data:")) return null
        val data = line.removePrefix("data:").trim()
        if (data.isEmpty()) return null
        return try {
            val evt = json.decodeFromString(AnthropicStreamEvent.serializer(), data)
            when (evt.type) {
                "content_block_delta" -> evt.delta?.text
                else -> null
            }
        } catch (_: Exception) { null }
    }

    // ============ Gemini ============

    private suspend fun sendGeminiMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): SendMessageResponse {
        val request = buildGeminiRequest(messages, temperature, maxTokens, systemPrompt)
        val url = "${endpoint.trimEnd('/')}/$model:generateContent"
        val response = api.geminiGenerateContent(url, apiKey, request)
        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string() ?: ""
            throw RuntimeException("HTTP ${response.code()}: $errBody".take(500))
        }
        val body = response.body() ?: throw RuntimeException("Empty Gemini response")
        val content = body.candidates?.firstOrNull()?.content?.parts?.joinToString("") { it.text ?: "" } ?: ""
        val usage = body.usageMetadata?.let {
            TokenUsage(it.promptTokenCount, it.candidatesTokenCount, it.totalTokenCount)
        }
        return SendMessageResponse(content = content, platform = AIPlatform.GEMINI, model = model, usage = usage)
    }

    private fun streamGemini(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        val request = buildGeminiRequest(messages, temperature, maxTokens, systemPrompt)
        val payload = json.encodeToString(GeminiRequest.serializer(), request)
        val url = "${endpoint.trimEnd('/')}/$model:streamGenerateContent?alt=sse&key=${apiKey}"
        val req = Request.Builder().url(url).post(payload.toRequestBody("application/json".toMediaType())).build()
        executeSse(req) { line ->
            parseGeminiStreamChunk(line, json)?.let { emit(it) }
        }
    }

    private fun buildGeminiRequest(
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): GeminiRequest {
        val contents = ContextManager.toApiMessages(messages).map { m ->
            val parts = mutableListOf<PartDto>()
            if (m.content.isNotBlank()) parts.add(PartDto(text = m.content))
            m.attachments.filter { it.type == AttachmentType.IMAGE && it.base64Data != null }.forEach { att ->
                parts.add(PartDto(inlineData = InlineDataDto(mimeType = att.mimeType, data = att.base64Data!!)))
            }
            ContentDto(parts = parts, role = if (m.role == MessageRole.USER) "user" else "model")
        }.filter { it.parts.isNotEmpty() }
        val sysContent = if (systemPrompt.isNotBlank()) ContentDto(parts = listOf(PartDto(text = systemPrompt))) else null
        return GeminiRequest(
            contents = contents,
            generationConfig = GenerationConfigDto(temperature = temperature, maxOutputTokens = maxTokens),
            systemInstruction = sysContent
        )
    }

    private fun parseGeminiStreamChunk(line: String, json: kotlinx.serialization.json.Json): String? {
        if (!line.startsWith("data:")) return null
        val data = line.removePrefix("data:").trim()
        if (data.isEmpty()) return null
        return try {
            val resp = json.decodeFromString(GeminiResponse.serializer(), data)
            resp.candidates?.firstOrNull()?.content?.parts?.joinToString("") { it.text ?: "" }?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    // ============ MiniMax（普通文本走 OpenAI 兼容；带图走 VLM） ============

    private suspend fun sendMiniMaxMessage(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): SendMessageResponse {
        // 若最后一条用户消息含图片附件，走 VLM 端点
        val lastUserMsg = messages.lastOrNull { it.role == MessageRole.USER }
        val hasImage = lastUserMsg?.attachments?.any { it.type == AttachmentType.IMAGE && it.base64Data != null } == true
        if (hasImage) {
            return sendMiniMaxVLMMessage(apiKey, model, lastUserMsg!!)
        }
        // 否则走 OpenAI 兼容
        return sendOpenAICompatibleMessage(AIPlatform.MINIMAX, apiKey, endpoint, model, messages, temperature, maxTokens, systemPrompt)
    }

    private suspend fun sendMiniMaxVLMMessage(apiKey: String, model: String, userMsg: ChatMessage): SendMessageResponse {
        val imageAtt = userMsg.attachments.first { it.type == AttachmentType.IMAGE && it.base64Data != null }
        val dataUri = "data:${imageAtt.mimeType};base64,${imageAtt.base64Data}"
        val vlmReq = MiniMaxVLMRequest(prompt = userMsg.content, imageUrl = dataUri)
        val vlmUrl = "https://api.minimax.chat/v1/coding_plan/vlm"
        val response = api.miniMaxVLM(vlmUrl, "Bearer $apiKey", vlmReq)
        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string() ?: ""
            throw RuntimeException("MiniMax VLM HTTP ${response.code()}: $errBody".take(500))
        }
        val body = response.body() ?: throw RuntimeException("Empty VLM response")
        val content = body.choices.firstOrNull()?.message?.content ?: ""
        return SendMessageResponse(content = content, platform = AIPlatform.MINIMAX, model = model, usage = null)
    }

    private fun streamMiniMax(
        apiKey: String,
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        // MiniMax 流式走 OpenAI 兼容（带 stream=true）
        val request = buildOpenAIRequest(model, messages, temperature, maxTokens, systemPrompt, stream = true)
        val payload = json.encodeToString(SimpleChatRequest.serializer(), request)
        val req = Request.Builder().url(endpoint).post(payload.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .build()
        executeSse(req) { line -> parseOpenAIStreamChunk(line)?.let { emit(it) } }
    }

    // ============ 自定义平台 ============

    private suspend fun sendCustomMessage(
        provider: CustomProvider,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): SendMessageResponse {
        val key = apiKey.ifBlank { provider.apiKey }
        return when (provider.apiStyle) {
            ApiStyle.OPENAI -> sendOpenAICompatibleMessage(AIPlatform.CUSTOM, key, provider.endpoint, model, messages, temperature, maxTokens, systemPrompt)
            ApiStyle.ANTHROPIC -> sendAnthropicMessage(key, provider.endpoint, model, messages, temperature, maxTokens, systemPrompt)
            ApiStyle.GEMINI -> sendGeminiMessage(key, provider.endpoint, model, messages, temperature, maxTokens, systemPrompt)
        }
    }

    // ============ SSE 通用执行 ============

    private suspend inline fun executeSse(request: Request, crossinline onLine: (String) -> Unit) = withContext(Dispatchers.IO) {
        val response: Response = httpClient.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                val errBody = resp.body?.string() ?: ""
                throw RuntimeException("SSE HTTP ${resp.code}: ${errBody.take(500)}")
            }
            val source = resp.body?.source() ?: throw RuntimeException("Empty stream body")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                logManager.stream(TAG, line.take(200))
                onLine(line)
            }
        }
    }

    // ============ 工具 ============

    private fun mapRole(role: MessageRole): String = when (role) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
    }
}
