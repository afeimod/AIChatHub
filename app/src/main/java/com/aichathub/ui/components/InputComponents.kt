package com.aichathub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.MessageAttachment

/**
 * 消息输入框（带附件 + 停止按钮）
 */
@Composable
fun MessageInputWithAttachment(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    isSending: Boolean,
    isStreaming: Boolean,
    activeAPIKey: com.aichathub.domain.model.APIKeyInfo?,
    pendingAttachments: List<MessageAttachment>,
    onRemoveAttachment: (String) -> Unit,
    onStopClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // 附件预览
            if (pendingAttachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pendingAttachments) { att ->
                        PendingAttachmentChip(att) { onRemoveAttachment(att.id) }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                FilledIconButton(
                    onClick = onAttachClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "附件")
                }
                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (isStreaming) {
                    FilledIconButton(
                        onClick = onStopClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "停止", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    FilledIconButton(
                        onClick = onSendClick,
                        enabled = (text.isNotBlank() || pendingAttachments.isNotEmpty()) && activeAPIKey != null && !isSending,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

@Composable
fun PendingAttachmentChip(att: MessageAttachment, onRemove: () -> Unit) {
    val icon: ImageVector = when (att.type) {
        com.aichathub.domain.model.AttachmentType.IMAGE -> Icons.Filled.Image
        com.aichathub.domain.model.AttachmentType.PDF -> Icons.Filled.PictureAsPdf
        com.aichathub.domain.model.AttachmentType.DOCUMENT -> Icons.Filled.Description
        com.aichathub.domain.model.AttachmentType.ARCHIVE -> Icons.Filled.FolderZip
        com.aichathub.domain.model.AttachmentType.AUDIO -> Icons.Filled.AudioFile
        com.aichathub.domain.model.AttachmentType.VIDEO -> Icons.Filled.VideoFile
        else -> Icons.Filled.InsertDriveFile
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (att.type == com.aichathub.domain.model.AttachmentType.IMAGE && att.base64Data != null) {
                AsyncImage(
                    model = "data:${att.mimeType};base64,${att.base64Data}",
                    contentDescription = att.fileName,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                )
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = att.fileName,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun AttachmentTypeDialog(
    onDismiss: () -> Unit,
    onImageSelected: () -> Unit,
    onDocumentSelected: () -> Unit,
    onArchiveSelected: () -> Unit,
    onCameraSelected: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择附件类型") },
        text = {
            Column {
                DialogItem(Icons.Filled.Image, "图片") { onImageSelected() }
                DialogItem(Icons.Filled.Description, "文档 (PDF/TXT/Word)") { onDocumentSelected() }
                DialogItem(Icons.Filled.FolderZip, "压缩包 (ZIP/RAR/7z)") { onArchiveSelected() }
                DialogItem(Icons.Filled.PhotoCamera, "拍照") { onCameraSelected() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DialogItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformSelector(
    selected: AIPlatform,
    onSelect: (AIPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("平台") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(getPlatformIcon(selected), contentDescription = null) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AIPlatform.builtIn.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName) },
                    onClick = { onSelect(p); expanded = false },
                    leadingIcon = { Icon(getPlatformIcon(p), contentDescription = null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selected: String,
    models: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    customInputEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var useCustom by remember { mutableStateOf(false) }
    var customModel by remember { mutableStateOf(selected) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = { onSelect(m); expanded = false }
                )
            }
            if (customInputEnabled) {
                Divider()
                DropdownMenuItem(
                    text = { Text("✎ 自定义模型名…") },
                    onClick = { useCustom = true; expanded = false }
                )
            }
        }
    }

    if (useCustom) {
        AlertDialog(
            onDismissRequest = { useCustom = false },
            title = { Text("自定义模型名") },
            text = {
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    label = { Text("模型 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customModel.isNotBlank()) onSelect(customModel.trim())
                    useCustom = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { useCustom = false }) { Text("取消") }
            }
        )
    }
}

fun getPlatformIcon(platform: AIPlatform): ImageVector = when (platform) {
    AIPlatform.DEEPSEEK -> Icons.Filled.Cloud
    AIPlatform.OPENAI -> Icons.Filled.Psychology
    AIPlatform.GEMINI -> Icons.Filled.AutoAwesome
    AIPlatform.ANTHROPIC -> Icons.Filled.SmartToy
    AIPlatform.MINIMAX -> Icons.Filled.PlayArrow
    AIPlatform.QWEN -> Icons.Filled.Translate
    AIPlatform.ZHIPU -> Icons.Filled.Lightbulb
    AIPlatform.MOONSHOT -> Icons.Filled.Nightlight
    AIPlatform.YI -> Icons.Filled.Language
    AIPlatform.BAICHUAN -> Icons.Filled.Waves
    AIPlatform.DOUBAO -> Icons.Filled.LocalFireDepartment
    AIPlatform.HUNYUAN -> Icons.Filled.WaterDrop
    AIPlatform.SPARK -> Icons.Filled.Bolt
    AIPlatform.SILICONFLOW -> Icons.Filled.Memory
    AIPlatform.GROQ -> Icons.Filled.Bolt
    AIPlatform.TOGETHER -> Icons.Filled.GroupWork
    AIPlatform.OPENROUTER -> Icons.Filled.Hub
    AIPlatform.CUSTOM -> Icons.Filled.Extension
}
