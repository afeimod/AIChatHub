package com.aichathub.domain.util

import com.aichathub.domain.model.ChatMessage
import com.aichathub.domain.model.ChatSession
import com.aichathub.domain.model.ContextStrategy
import com.aichathub.domain.model.MessageAttachment
import com.aichathub.domain.model.MessageRole

/**
 * Token 数估算器（粗略：中文 ~1.5 字符/token，英文 ~4 字符/token，平均 2.5 字符/token）
 */
object TokenEstimator {

    fun estimateText(text: String): Int {
        if (text.isEmpty()) return 0
        var tokens = 0
        for (ch in text) {
            tokens += if (ch.code > 127) 2 else 1
        }
        return (tokens / 2.5).toInt().coerceAtLeast(1)
    }

    fun estimateMessage(message: ChatMessage): Int {
        var total = estimateText(message.content) + 4
        for (att in message.attachments) {
            total += estimateAttachment(att)
        }
        return total
    }

    fun estimateAttachment(att: MessageAttachment): Int {
        return when (att.type) {
            com.aichathub.domain.model.AttachmentType.IMAGE -> 85
            else -> {
                val dataLen = att.base64Data?.length ?: 0
                (dataLen / 3 / 4).coerceAtMost(8000)
            }
        }
    }

    fun estimateSession(session: ChatSession): Int {
        var total = if (session.systemPrompt.isNotBlank()) estimateText(session.systemPrompt) else 0
        session.messages.forEach { total += estimateMessage(it) }
        return total
    }
}

/**
 * 上下文窗口管理器 — 根据 strategy 修剪消息历史
 */
object ContextManager {

    fun trim(session: ChatSession): List<ChatMessage> {
        val all = session.messages
        if (all.isEmpty()) return emptyList()

        return when (session.contextStrategy) {
            ContextStrategy.UNLIMITED -> all
            ContextStrategy.SYSTEM_ONLY -> all.takeLast(1)
            ContextStrategy.SLIDING_WINDOW -> all.takeLast(20)
            ContextStrategy.SUMMARIZE -> {
                if (all.size <= 20) all
                else listOf(all.first()) + all.takeLast(19)
            }
        }
    }

    fun estimatedTokens(session: ChatSession): Int {
        val system = if (session.systemPrompt.isNotBlank()) TokenEstimator.estimateText(session.systemPrompt) else 0
        val messages = trim(session).sumOf { TokenEstimator.estimateMessage(it) }
        return system + messages
    }

    fun toApiMessages(messages: List<ChatMessage>): List<ChatMessage> =
        messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
}
