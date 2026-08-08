package com.aichathub.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aichathub.domain.model.AttachmentType
import com.aichathub.domain.model.ChatMessage
import com.aichathub.domain.model.MessageRole
import com.aichathub.domain.model.TokenUsage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    isLastAssistant: Boolean = false,
    onCopy: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onDelete: () -> Unit = {},
    enableMarkdown: Boolean = true,
    fontSizeScale: Float = 1.0f
) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else if (message.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Filled.Person else Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = if (isUser) "You" else "AI Assistant",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )
            if (message.isStreaming) {
                Text(
                    text = "streaming…",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            message.usage?.let {
                Text(
                    text = "${it.totalTokens} tok",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Attachments
        if (message.attachments.isNotEmpty()) {
            AttachmentRow(message.attachments, isUser)
        }

        // Bubble
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (message.content.isBlank() && message.isStreaming) {
                TypingIndicator()
            } else if (enableMarkdown && !isUser && !message.isError) {
                MarkdownText(
                    markdown = message.content,
                    fontSize = (15 * fontSizeScale).toInt(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = message.content,
                    fontSize = (15 * fontSizeScale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (isUser) FontFamily.Default else FontFamily.Default
                )
            }
        }

        // Footer: timestamp + actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Copy
            IconButton(onClick = { onCopy(message.content) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            // Regenerate (only for last assistant message)
            if (isLastAssistant && !message.isStreaming) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重新生成", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachments: List<com.aichathub.domain.model.MessageAttachment>, isUser: Boolean) {
    LazyRow(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .widthIn(max = 320.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(attachments) { att ->
            when (att.type) {
                AttachmentType.IMAGE -> {
                    val dataUri = att.base64Data?.let { "data:${att.mimeType};base64,$it" }
                    AsyncImage(
                        model = dataUri ?: att.url,
                        contentDescription = att.fileName,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                else -> AttachmentChip(att)
            }
        }
    }
}

@Composable
fun AttachmentChip(att: com.aichathub.domain.model.MessageAttachment) {
    val icon = when (att.type) {
        AttachmentType.PDF -> Icons.Filled.PictureAsPdf
        AttachmentType.DOCUMENT -> Icons.Filled.Description
        AttachmentType.ARCHIVE -> Icons.Filled.FolderZip
        AttachmentType.AUDIO -> Icons.Filled.AudioFile
        AttachmentType.VIDEO -> Icons.Filled.VideoFile
        AttachmentType.IMAGE -> Icons.Filled.Image
        else -> Icons.Filled.InsertDriveFile
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = att.fileName,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatSize(att.size),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f + 0.3f * (if (i == 0) scale else 1f - scale)))
            )
        }
    }
}

@Composable
fun EmptyChatView(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "开始与 AI 对话",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "选择平台与模型，输入消息即可开始",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStart) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("开始新对话")
        }
    }
}

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
    else -> "%.1fMB".format(bytes / 1024.0 / 1024.0)
}

/** Helper for UI: copy text to system clipboard */
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AIChatHub", text))
}
