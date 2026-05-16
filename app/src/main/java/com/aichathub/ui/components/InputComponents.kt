package com.aichathub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.aichathub.ui.theme.PrimaryBlue
import com.aichathub.ui.theme.TextSecondary

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "输入您的消息...",
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
                    onSend = { if (text.isNotBlank()) onSend() }
                ),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送"
                )
            }
        }
    }
}

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
                    imageVector = com.aichathub.ui.components.getPlatformIcon(selectedPlatform),
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
                                imageVector = com.aichathub.ui.components.getPlatformIcon(platform),
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

object getPlatformIcon {
    @Composable
    operator fun invoke(platform: com.aichathub.domain.model.AIPlatform) = when (platform) {
        com.aichathub.domain.model.AIPlatform.DEEPSEEK -> androidx.compose.material.icons.Icons.Default.Cloud
        com.aichathub.domain.model.AIPlatform.MINIMAX -> androidx.compose.material.icons.Icons.Default.PlayArrow
        com.aichathub.domain.model.AIPlatform.OPENAI -> androidx.compose.material.icons.Icons.Default.Psychology
        com.aichathub.domain.model.AIPlatform.GEMINI -> androidx.compose.material.icons.Icons.Default.AutoAwesome
    }
}