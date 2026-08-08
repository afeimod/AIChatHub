package com.aichathub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.aichathub.domain.model.MessageRole
import com.aichathub.ui.components.*
import com.aichathub.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToAPIKeys: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showPlatformMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // 自动滚动到最后一条消息
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { viewModel.showSessionHistory(true) }) {
                        Text(
                            text = uiState.currentSession?.title ?: "AI Chat Hub",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.selectedPlatform.displayName} · ${uiState.selectedModel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showSessionHistory(true) }) {
                        Icon(Icons.Filled.Menu, contentDescription = "会话历史")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToTerminal) {
                        Icon(Icons.Filled.Terminal, contentDescription = "终端")
                    }
                    IconButton(onClick = onNavigateToWorkspace) {
                        Icon(Icons.Filled.Folder, contentDescription = "工作目录")
                    }
                    IconButton(onClick = { viewModel.showSystemPromptDialog(true) }) {
                        Icon(Icons.Filled.EditNote, contentDescription = "系统提示")
                    }
                    IconButton(onClick = { viewModel.createNewSession() }) {
                        Icon(Icons.Filled.Add, contentDescription = "新对话")
                    }
                    IconButton(onClick = onNavigateToAPIKeys) {
                        Icon(Icons.Filled.Key, contentDescription = "API Key")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputWithAttachment(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSendClick = { viewModel.sendMessage() },
                onAttachClick = { showAttachmentDialog = true },
                isSending = uiState.isLoading,
                isStreaming = uiState.isStreaming,
                activeAPIKey = uiState.activeAPIKey,
                pendingAttachments = uiState.pendingAttachments,
                onRemoveAttachment = viewModel::removeAttachment,
                onStopClick = viewModel::stopGeneration
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // API Key 缺失提示
            if (uiState.activeAPIKey == null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("未配置 API Key", modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToAPIKeys) { Text("去配置") }
                    }
                }
            }

            // 平台 & 模型选择
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformSelector(
                    selected = uiState.selectedPlatform,
                    onSelect = viewModel::selectPlatform,
                    modifier = Modifier.weight(1f)
                )
                ModelSelector(
                    selected = uiState.selectedModel,
                    models = viewModel.getAvailableModels(),
                    onSelect = viewModel::selectModel,
                    modifier = Modifier.weight(1f)
                )
            }

            // Token 估算
            if (uiState.settings.enableTokenCounter) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "输入: ${uiState.estimatedInputTokens} tok",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "会话: ${uiState.estimatedSessionTokens} tok",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // 消息列表
            if (uiState.messages.isEmpty()) {
                EmptyChatView(onStart = { viewModel.createNewSession() })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        val isLastAssistant = msg.role == MessageRole.ASSISTANT &&
                            uiState.messages.lastOrNull { it.role == MessageRole.ASSISTANT }?.id == msg.id
                        ChatBubble(
                            message = msg,
                            isLastAssistant = isLastAssistant,
                            onCopy = { content ->
                                copyToClipboard(context, content)
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                viewModel.copyMessage(content)
                            },
                            onRegenerate = { viewModel.regenerateMessage(msg.id) },
                            onDelete = { viewModel.deleteMessage(msg.id) },
                            enableMarkdown = uiState.settings.enableMarkdown,
                            fontSizeScale = uiState.settings.fontSizeScale
                        )
                    }
                }
            }
        }
    }

    // 附件类型选择对话框
    if (showAttachmentDialog) {
        AttachmentTypeDialog(
            onDismiss = { showAttachmentDialog = false },
            onImageSelected = { imagePicker.launch("image/*"); showAttachmentDialog = false },
            onDocumentSelected = { docPicker.launch("*/*"); showAttachmentDialog = false },
            onArchiveSelected = { docPicker.launch("*/*"); showAttachmentDialog = false },
            onCameraSelected = {
                Toast.makeText(context, "相机功能开发中", Toast.LENGTH_SHORT).show()
                showAttachmentDialog = false
            }
        )
    }

    // 会话历史
    if (uiState.showSessionHistory) {
        SessionHistoryDialog(
            sessions = uiState.allSessions,
            onSelect = { viewModel.selectSession(it) },
            onDelete = { viewModel.deleteSession(it.id) },
            onNew = { viewModel.createNewSession() },
            onDismiss = { viewModel.showSessionHistory(false) }
        )
    }

    // 系统提示编辑
    if (uiState.showSystemPromptDialog) {
        SystemPromptDialog(
            initialText = uiState.systemPrompt,
            onConfirm = { viewModel.updateSystemPrompt(it); viewModel.showSystemPromptDialog(false) },
            onDismiss = { viewModel.showSystemPromptDialog(false) }
        )
    }
}

@Composable
private fun SessionHistoryDialog(
    sessions: List<com.aichathub.domain.model.ChatSession>,
    onSelect: (com.aichathub.domain.model.ChatSession) -> Unit,
    onDelete: (com.aichathub.domain.model.ChatSession) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<com.aichathub.domain.model.ChatSession?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("会话历史", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (sessions.isEmpty()) {
                    Text("暂无会话", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).clickable { onSelect(session) }) {
                                        Text(session.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${session.platform.displayName} · ${session.messages.size} 条", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    IconButton(onClick = { deleteTarget = session }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onNew(); onDismiss() }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建")
                    }
                }
            }
        }
    }
    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除会话") },
            text = { Text("确定要删除 \"${s.title}\" 吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(s); deleteTarget = null }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SystemPromptDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("系统提示词 (System Prompt)") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("输入系统提示词，可影响 AI 的角色与行为…") },
                minLines = 4,
                maxLines = 8
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
