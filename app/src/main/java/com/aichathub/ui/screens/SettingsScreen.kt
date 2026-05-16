package com.aichathub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichathub.domain.model.AIPlatform
import com.aichathub.ui.theme.*
import com.aichathub.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = TextOnPrimary,
                    navigationIconContentColor = TextOnPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 外观设置
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "暗色模式",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "切换应用主题",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.settings.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 默认参数设置
            Text(
                text = "默认参数",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 默认平台
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "默认平台",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = uiState.settings.defaultPlatform.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Temperature
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Thermostat, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Temperature",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "控制回复的随机性",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = String.format("%.1f", uiState.settings.defaultTemperature),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.settings.defaultTemperature,
                        onValueChange = { viewModel.updateDefaultTemperature(it) },
                        valueRange = 0f..2f,
                        steps = 19
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Max Tokens
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "最大Token数",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "限制回复长度",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = uiState.settings.defaultMaxTokens.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = uiState.settings.defaultMaxTokens.toFloat(),
                        onValueChange = { viewModel.updateDefaultMaxTokens(it.toInt()) },
                        valueRange = 256f..8192f,
                        steps = 30
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 数据管理
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "清空对话历史",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "删除所有会话记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { viewModel.showClearConfirmDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 关于
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Chat Hub",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "版本 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持DeepSeek、MiniMax、OpenAI、Google Gemini等平台的多功能AI对话客户端。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 确认清空对话框
        if (uiState.showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideClearConfirmDialog() },
                title = { Text("清空确认") },
                text = { Text("确定要清空所有对话历史吗？此操作不可撤销。") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearAllSessions() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideClearConfirmDialog() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}