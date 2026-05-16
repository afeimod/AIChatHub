package com.aichathub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.aichathub.ui.components.*
import com.aichathub.ui.theme.*
import com.aichathub.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToAPIKeys: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.selectedPlatform.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.selectedModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToAPIKeys) {
                        Icon(Icons.Default.Key, contentDescription = "API Keys")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = TextOnPrimary,
                    navigationIconContentColor = TextOnPrimary,
                    actionIconContentColor = TextOnPrimary
                )
            )
        },
        bottomBar = {
            MessageInput(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                enabled = !uiState.isLoading && uiState.activeAPIKey != null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 平台选择器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformSelector(
                    selectedPlatform = uiState.selectedPlatform,
                    onPlatformChange = viewModel::selectPlatform,
                    modifier = Modifier.weight(1f)
                )
                ModelSelector(
                    selectedModel = uiState.selectedModel,
                    availableModels = viewModel.getAvailableModels(uiState.selectedPlatform),
                    onModelChange = viewModel::selectModel,
                    modifier = Modifier.weight(1f)
                )
            }

            // 消息列表
            if (uiState.messages.isEmpty()) {
                EmptyChatView(
                    modifier = Modifier.weight(1f),
                    onStartChat = { viewModel.createNewSession() }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(message = message)
                    }

                    if (uiState.isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }

            // 错误提示
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // API Key 提示
            if (uiState.activeAPIKey == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "请先配置API密钥",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onNavigateToAPIKeys) {
                            Text("去配置")
                        }
                    }
                }
            }
        }
    }
}