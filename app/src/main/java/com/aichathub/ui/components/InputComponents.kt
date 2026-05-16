package com.aichathub.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aichathub.domain.model.AttachmentType
import com.aichathub.domain.model.MessageAttachment
import com.aichathub.ui.theme.PrimaryBlue
import com.aichathub.ui.theme.TextSecondary

/**
 * 带文件附件功能的输入组件
 */
@Composable
fun MessageInputWithAttachment(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    enabled: Boolean = true,
    attachments: List<MessageAttachment> = emptyList(),
    onRemoveAttachment: (MessageAttachment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 显示已添加的附件
            if (attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { attachment ->
                        AttachmentChip(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment) }
                        )
                    }
                }
            }

            // 主输入行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 附件按钮 - 更改为加号图标，更醒目
                FilledIconButton(
                    onClick = onAttachFile,
                    enabled = enabled,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f),
                        contentColor = PrimaryBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加附件",
                        modifier = Modifier.size(24.dp)
                    )
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (attachments.isNotEmpty()) "添加消息描述..." else "输入您的消息...",
                            color = TextSecondary
                        )
                    },
                    enabled = enabled,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray,
                        disabledBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (text.isNotBlank() || attachments.isNotEmpty()) onSend() }
                    ),
                    singleLine = false,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = enabled && (text.isNotBlank() || attachments.isNotEmpty()),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            }
        }
    }
}

/**
 * 附件展示组件
 */
@Composable
fun AttachmentChip(
    attachment: MessageAttachment,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (attachment.type) {
                    AttachmentType.IMAGE -> Icons.Default.Image
                    AttachmentType.PDF -> Icons.Default.PictureAsPdf
                    AttachmentType.DOCUMENT -> Icons.Default.Description
                    AttachmentType.OTHER -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * 图片附件预览
 */
@Composable
fun AttachmentImagePreview(
    attachment: MessageAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* 查看大图 */ }
    ) {
        AsyncImage(
            model = attachment.uri,
            contentDescription = attachment.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 移除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .padding(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 文件类型选择器
 */
@Composable
fun AttachmentTypeDialog(
    onDismiss: () -> Unit,
    onSelectImage: () -> Unit,
    onSelectDocument: () -> Unit,
    onSelectCamera: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文件类型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("图片") },
                    leadingContent = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onSelectImage()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text("文档") },
                    leadingContent = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onSelectDocument()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onSelectCamera()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformSelector(
    selectedPlatform: com.aichathub.domain.model.AIPlatform,
    onPlatformChange: (com.aichathub.domain.model.AIPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedPlatform.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("选择平台") },
            leadingIcon = {
                Icon(
                    imageVector = getPlatformIcon(selectedPlatform),
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            com.aichathub.domain.model.AIPlatform.entries.forEach { platform ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getPlatformIcon(platform),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(platform.displayName)
                        }
                    },
                    onClick = {
                        onPlatformChange(platform)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: String,
    availableModels: List<String>,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("选择模型") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelChange(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun getPlatformIcon(platform: com.aichathub.domain.model.AIPlatform) = when (platform) {
    com.aichathub.domain.model.AIPlatform.DEEPSEEK -> Icons.Filled.Cloud
    com.aichathub.domain.model.AIPlatform.MINIMAX -> Icons.Filled.PlayArrow
    com.aichathub.domain.model.AIPlatform.OPENAI -> Icons.Filled.Psychology
    com.aichathub.domain.model.AIPlatform.GEMINI -> Icons.Filled.AutoAwesome
}