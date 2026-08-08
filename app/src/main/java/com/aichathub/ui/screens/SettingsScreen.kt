package com.aichathub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.ContextStrategy
import com.aichathub.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCustomProviders: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============ 外观 ============
            SettingsSectionHeader("外观")
            Card {
                Column {
                    SwitchItem(
                        title = "深色模式",
                        subtitle = "切换应用的明暗主题",
                        icon = Icons.Filled.DarkMode,
                        checked = settings.isDarkMode,
                        onChange = { viewModel.toggleDarkMode() }
                    )
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("字体大小", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = settings.fontSizeScale,
                            onValueChange = viewModel::updateFontSizeScale,
                            valueRange = 0.7f..1.5f,
                            steps = 7
                        )
                        Text("当前: ${"%.1f".format(settings.fontSizeScale)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    HorizontalDivider()
                    SwitchItem(
                        title = "Markdown 渲染",
                        subtitle = "在 AI 回复中渲染格式化文本与代码块",
                        icon = Icons.Filled.Code,
                        checked = settings.enableMarkdown,
                        onChange = { viewModel.toggleMarkdown() }
                    )
                }
            }

            // ============ 默认参数 ============
            SettingsSectionHeader("默认参数")
            Card {
                Column {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("默认平台", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        AIPlatform.builtIn.chunked(3).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { p ->
                                    FilterChip(
                                        selected = settings.defaultPlatform == p,
                                        onClick = { viewModel.updateDefaultPlatform(p) },
                                        label = { Text(p.displayName, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                    HorizontalDivider()
                    SliderItem(
                        title = "Temperature",
                        subtitle = "值越高回复越发散",
                        value = settings.defaultTemperature,
                        range = 0f..2f,
                        steps = 19,
                        onChange = viewModel::updateDefaultTemperature,
                        display = "%.2f"
                    )
                    HorizontalDivider()
                    SliderItem(
                        title = "Max Tokens",
                        subtitle = "单次回复最大长度",
                        value = settings.defaultMaxTokens.toFloat(),
                        range = 256f..8192f,
                        steps = 30,
                        onChange = { viewModel.updateDefaultMaxTokens(it.toInt()) },
                        display = "%.0f"
                    )
                }
            }

            // ============ 上下文管理 ============
            SettingsSectionHeader("上下文管理（无限制 / 智能截断）")
            Card {
                Column {
                    ContextStrategy.values().forEach { strategy ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.updateContextStrategy(strategy) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = settings.defaultContextStrategy == strategy, onClick = { viewModel.updateContextStrategy(strategy) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strategy.displayName, fontWeight = FontWeight.Medium)
                                Text(strategy.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                    HorizontalDivider()
                    SliderItem(
                        title = "最大上下文 Token",
                        subtitle = "UNLIMITED 模式下的提示阈值",
                        value = settings.defaultContextMaxTokens.toFloat(),
                        range = 4096f..200000f,
                        steps = 50,
                        onChange = { viewModel.updateContextMaxTokens(it.toInt()) },
                        display = "%.0f"
                    )
                    HorizontalDivider()
                    SwitchItem(
                        title = "Token 计数器",
                        subtitle = "在聊天界面显示输入/会话 token 估算",
                        icon = Icons.Filled.QueryStats,
                        checked = settings.enableTokenCounter,
                        onChange = { viewModel.toggleTokenCounter() }
                    )
                }
            }

            // ============ AI 行为 ============
            SettingsSectionHeader("AI 行为")
            Card {
                Column {
                    SwitchItem(
                        title = "流式响应 (SSE)",
                        subtitle = "实时逐 token 显示 AI 回复",
                        icon = Icons.Filled.Stream,
                        checked = settings.enableStreamResponse,
                        onChange = { viewModel.toggleStreamResponse() }
                    )
                    HorizontalDivider()
                    SwitchItem(
                        title = "多模态输入",
                        subtitle = "支持发送图片与文档附件",
                        icon = Icons.Filled.Image,
                        checked = settings.enableMultimodal,
                        onChange = { viewModel.toggleMultimodal() }
                    )
                    HorizontalDivider()
                    SwitchItem(
                        title = "自动生成标题",
                        subtitle = "使用第一条消息作为会话标题",
                        icon = Icons.Filled.Title,
                        checked = settings.autoTitleFromFirstMessage,
                        onChange = { viewModel.toggleAutoTitle() }
                    )
                }
            }

            // ============ 高级 ============
            SettingsSectionHeader("高级")
            Card {
                Column {
                    SwitchItem(
                        title = "终端日志",
                        subtitle = "记录 API 请求/响应、流式数据、错误",
                        icon = Icons.Filled.Terminal,
                        checked = settings.enableTerminalLog,
                        onChange = { viewModel.toggleTerminalLog() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("自定义平台") },
                        supportingContent = { Text("添加 OpenAI 兼容端点") },
                        leadingContent = { Icon(Icons.Filled.Extension, null) },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier = Modifier.clickable { onNavigateToCustomProviders() }
                    )
                    HorizontalDivider()
                    SliderItem(
                        title = "最大历史会话数",
                        subtitle = "超过此数量会自动清理最旧的会话",
                        value = settings.maxHistorySessions.toFloat(),
                        range = 10f..500f,
                        steps = 49,
                        onChange = { viewModel.updateMaxHistorySessions(it.toInt()) },
                        display = "%.0f"
                    )
                }
            }

            // ============ 数据管理 ============
            SettingsSectionHeader("数据管理")
            Card {
                Column {
                    ListItem(
                        headlineContent = { Text("清空对话历史", color = MaterialTheme.colorScheme.error) },
                        supportingContent = { Text("永久删除所有会话与消息") },
                        leadingContent = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { viewModel.showClearConfirmDialog() }
                    )
                }
            }

            // ============ 关于 ============
            SettingsSectionHeader("关于")
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Chat Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("版本 2.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "支持 18+ AI 平台的统一聊天客户端。DeepSeek · OpenAI · Gemini · Anthropic · 通义千问 · 智谱 · Moonshot · 零一万物 · 百川 · 豆包 · 混元 · 星火 · SiliconFlow · Groq · Together · OpenRouter · MiniMax · 自定义平台。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideClearConfirmDialog,
            title = { Text("清空对话历史") },
            text = { Text("此操作不可撤销，所有会话都将被删除。") },
            confirmButton = {
                TextButton(onClick = viewModel::clearAllSessions) { Text("确定清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideClearConfirmDialog) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, fontSize = 11.sp) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = { onChange() }) }
    )
}

@Composable
private fun SliderItem(title: String, subtitle: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit, display: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
        Text(display.format(value), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

