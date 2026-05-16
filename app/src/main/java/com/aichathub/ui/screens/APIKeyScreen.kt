package com.aichathub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aichathub.domain.model.APIKeyInfo
import com.aichathub.ui.components.getPlatformIcon
import com.aichathub.ui.theme.*
import com.aichathub.ui.viewmodel.APIKeyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeyScreen(
    onNavigateBack: () -> Unit,
    viewModel: APIKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API密钥管理") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加密钥", tint = TextOnPrimary)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.apiKeys.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = PrimaryBlue.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无API密钥",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击 + 按钮添加新的API密钥",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.apiKeys) { key ->
                        APIKeyCard(
                            apiKey = key,
                            onSetActive = { viewModel.setActiveAPIKey(key.id) },
                            onDelete = { viewModel.deleteAPIKey(key.id) },
                            onTest = {
                                viewModel.testConnection(
                                    key.platform,
                                    key.apiKey.ifBlank { "test" }, // 实际密钥需要从存储获取
                                    key.platform.defaultModel
                                )
                            }
                        )
                    }
                }
            }
        }

        // 添加密钥对话框
        if (uiState.showAddDialog) {
            AddAPIKeyDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { platform, apiKey, name ->
                    viewModel.addAPIKey(platform, apiKey, name)
                },
                isLoading = uiState.isLoading
            )
        }

        // 测试结果提示
        uiState.testResult?.let { result ->
            LaunchedEffect(result) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearTestResult()
            }
        }
    }
}

@Composable
fun APIKeyCard(
    apiKey: APIKeyInfo,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getPlatformIcon(apiKey.platform),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = when (apiKey.platform) {
                        AIPlatform.DEEPSEEK -> DeepSeekColor
                        AIPlatform.MINIMAX -> MiniMaxColor
                        AIPlatform.OPENAI -> OpenAIColor
                        AIPlatform.GEMINI -> GeminiColor
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apiKey.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = apiKey.platform.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (apiKey.isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text("活跃") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!apiKey.isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设为活跃")
                    }
                }

                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("测试")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除确认") },
            text = { Text("确定要删除 ${apiKey.name} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAPIKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (AIPlatform, String, String) -> Unit,
    isLoading: Boolean
) {
    var selectedPlatform by remember { mutableStateOf(AIPlatform.DEEPSEEK) }
    var apiKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("添加API密钥") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 平台选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPlatform.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择平台") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AIPlatform.entries.forEach { platform ->
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
                                    selectedPlatform = platform
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // API密钥输入
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API密钥") },
                    placeholder = { Text("请输入API密钥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 名称输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（可选）") },
                    placeholder = { Text("给这个密钥起个名字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPlatform, apiKey, name) },
                enabled = apiKey.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("添加")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}