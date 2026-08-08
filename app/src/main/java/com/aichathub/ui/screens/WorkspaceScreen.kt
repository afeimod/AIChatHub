package com.aichathub.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichathub.domain.model.WorkspaceFile
import com.aichathub.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    onBack: () -> Unit,
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pickDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, it)
                viewModel.setCustomDirectory(it, docFile?.name ?: "自定义目录")
            } catch (e: Exception) {
                Toast.makeText(context, "获取目录权限失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("工作目录") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshFiles) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // 当前目录信息
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前工作目录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(uiState.settings.displayName, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = uiState.settings.useDefaultDownload,
                            onCheckedChange = { viewModel.setUseDefaultDownload(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("使用默认下载目录 (Download)", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { pickDirLauncher.launch(null) },
                        enabled = !uiState.settings.useDefaultDownload
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择自定义目录")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = uiState.settings.autoSaveAIResponse,
                            onCheckedChange = { viewModel.setAutoSave(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("自动保存 AI 回复到工作目录", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = uiState.settings.filePrefix,
                        onValueChange = viewModel::setFilePrefix,
                        label = { Text("文件名前缀") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }

            // 文件列表
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("文件列表 (${uiState.files.size})", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("目录为空", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.files, key = { it.path }) { file ->
                        FileItem(file = file, onDelete = { viewModel.deleteFile(file.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(file: WorkspaceFile, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else getFileIcon(file.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatSize(file.size)} · ${formatDate(file.lastModified)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (!file.isDirectory) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件") },
            text = { Text("确定要删除 \"${file.name}\" 吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

private fun getFileIcon(name: String) = when {
    name.endsWith(".txt", true) || name.endsWith(".md", true) -> Icons.Filled.Description
    name.endsWith(".pdf", true) -> Icons.Filled.PictureAsPdf
    name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".gif", true) -> Icons.Filled.Image
    name.endsWith(".json", true) || name.endsWith(".xml", true) -> Icons.Filled.Code
    name.endsWith(".zip", true) || name.endsWith(".rar", true) || name.endsWith(".7z", true) -> Icons.Filled.FolderZip
    else -> Icons.Filled.InsertDriveFile
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
    else -> "%.1fMB".format(bytes / 1024.0 / 1024.0)
}

private fun formatDate(ts: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
