package com.aichathub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aichathub.domain.model.ChatMessage
import com.aichathub.domain.model.MessageRole
import com.aichathub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) UserMessageBg else (AIMessageBg)
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isUser) PrimaryBlue else SecondaryTeal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "You" else "AI Assistant",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
                .widthIn(max = 320.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = SecondaryTeal
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AIMessageBg)
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryBlue.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatView(
    modifier: Modifier = Modifier,
    onStartChat: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = PrimaryBlue.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "开始与 AI 对话",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "选择 AI 平台并输入您的问题",
            fontSize = 16.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartChat,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            )
        ) {
            Text("开始新对话")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}