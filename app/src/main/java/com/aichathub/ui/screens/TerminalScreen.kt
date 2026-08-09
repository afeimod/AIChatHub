package com.aichathub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichathub.domain.model.LogLevel
import com.aichathub.domain.model.TerminalLog
import com.aichathub.ui.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val logs = remember(uiState.logs, uiState.liveLogs, uiState.filterLevel, uiState.filterText) {
        viewModel.filteredLogs()
    }

    // 自动滚动到底部
    LaunchedEffect(logs.size) {
        if (uiState.autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端输出") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePause) {
                        Icon(if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = "暂停/继续")
                    }
                    IconButton(onClick = viewModel::toggleAutoScroll) {
                        Icon(if (uiState.autoScroll) Icons.Filled.VerticalAlignBottom else Icons.Filled.ArrowDownward, contentDescription = "自动滚动")
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(logs.joinToString("\n") { formatLogLine(it) })) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "复制全部")
                    }
                    IconButton(onClick = viewModel::clearLogs) {
                        Icon(Icons.Filled.Delete, contentDescription = "清空")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 过滤栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filterText,
                    onValueChange = viewModel::setFilterText,
                    placeholder = { Text("过滤…", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = uiState.filterLevel == null,
                    onClick = { viewModel.setFilterLevel(null) },
                    label = { Text("全部", fontSize = 11.sp) }
                )
                LogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = uiState.filterLevel == level,
                        onClick = { viewModel.setFilterLevel(level) },
                        label = { Text(level.displayName, fontSize = 11.sp, color = levelColor(level)) }
                    )
                }
            }

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("共 ${logs.size} 条", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(if (uiState.isPaused) "已暂停" else "实时", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }

            // 日志列表
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))
            ) {
                items(logs, key = { it.id }) { log ->
                    TerminalLogItem(log = log, showTimestamp = uiState.showTimestamp)
                }
                if (logs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无日志输出\n\n发送消息后，API 请求、响应、流式 chunk 与错误信息将显示在这里",
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogItem(log: TerminalLog, showTimestamp: Boolean) {
    val color = levelColor(log.level)
    val bg = when (log.level) {
        LogLevel.ERROR -> Color(0x40FF0000)
        LogLevel.WARN -> Color(0x40FFA500)
        LogLevel.STREAM -> Color(0x1000FF00)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (showTimestamp) {
            Text(
                text = formatTime(log.timestamp),
                color = Color(0xFF888888),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        Text(
            text = "[${log.level.symbol}]",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp)
        )
        if (log.tag.isNotBlank()) {
            Text(
                text = "${log.tag}:",
                color = Color(0xFFA0A0FF),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = log.message,
            color = Color(0xFFD4D4D4),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.INFO -> Color(0xFF4FC3F7)
    LogLevel.REQUEST -> Color(0xFFFFD54F)
    LogLevel.RESPONSE -> Color(0xFF81C784)
    LogLevel.STREAM -> Color(0xFFCE93D8)
    LogLevel.ERROR -> Color(0xFFEF5350)
    LogLevel.WARN -> Color(0xFFFFB74D)
    LogLevel.DEBUG -> Color(0xFFB0BEC5)
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(ts))

private fun formatLogLine(log: TerminalLog): String {
    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
    return "$ts [${log.level.symbol}] ${log.tag}: ${log.message}"
}
