package com.aichathub.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.ChatSession
import com.aichathub.domain.model.MessageAttachment
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
    
    // 会话历史对话框状态
    var showSessionDialog by remember { mutableStateOf(false) }

    // 文件选择器
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // 图片选择器
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // 显示附件类型选择对话框
    var showAttachmentDialog by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSessionDialog = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "历史记录")
                        }
                        IconButton(onClick = onNavigateToAPIKeys) {
                            Icon(Icons.Default.Key, contentDescription = "API配置")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建对话")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
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
            MessageInputWithAttachment(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onAttachFile = { showAttachmentDialog = true },
                enabled = !uiState.isLoading && uiState.activeAPIKey != null,
                attachments = uiState.pendingAttachments,
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                modifier = Modifier.navigationBarsPadding()
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
                            Text("关闭")
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

    // 附件类型选择对话框
    if (showAttachmentDialog) {
        AttachmentTypeDialog(
            onDismiss = { showAttachmentDialog = false },
            onSelectImage = {
                imageLauncher.launch("image/*")
            },
            onSelectDocument = {
                documentLauncher.launch("*/*")
            },
            onSelectCamera = {
                // TODO: 实现拍照功能
            },
            onSelectArchive = {
                // 使用通用类型选择器，让用户可以选中有压缩包文件
                documentLauncher.launch("*/*")
            }
        )
    }

    // 会话历史对话框
    if (showSessionDialog) {
        SessionHistoryDialog(
            sessions = uiState.allSessions,
            currentSessionId = uiState.currentSession?.id,
            onSessionSelect = { session ->
                viewModel.selectSession(session)
                showSessionDialog = false
            },
            onSessionDelete = { sessionId ->
                viewModel.deleteSession(sessionId)
            },
            onDismiss = { showSessionDialog = false }
        )
    }
}

/**
 * 会话历史对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryDialog(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    onSessionSelect: (ChatSession) -> Unit,
    onSessionDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史对话") },
        text = {
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无历史对话",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(sessions) { session ->
                        SessionItem(
                            session = session,
                            isSelected = session.id == currentSessionId,
                            onClick = { onSessionSelect(session) },
                            onDelete = { onSessionDelete(session.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 会话列表项
 */
@Composable
fun SessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 会话图标
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    TextSecondary
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 会话信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${session.platform.displayName} · ${session.messages.size}条消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            // 删除按钮
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = TextSecondary
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除对话") },
            text = { Text("确定要删除这个对话吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
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