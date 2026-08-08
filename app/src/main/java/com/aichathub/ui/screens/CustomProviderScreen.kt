package com.aichathub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichathub.domain.model.ApiStyle
import com.aichathub.domain.model.CustomProvider
import com.aichathub.ui.viewmodel.CustomProviderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProviderScreen(
    onBack: () -> Unit,
    viewModel: CustomProviderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() }
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义平台") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 说明
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("自定义平台", fontWeight = FontWeight.Bold)
                    Text(
                        "添加任意 OpenAI / Anthropic / Gemini 兼容的 API 端点。适用于自部署模型、代理服务、企业内部 API 等。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (uiState.providers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Extension, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("尚无自定义平台", fontWeight = FontWeight.Bold)
                        Text("点击右下角 + 添加", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.providers, key = { it.id }) { provider ->
                        CustomProviderCard(
                            provider = provider,
                            onEdit = { viewModel.showEditDialog(provider) },
                            onDelete = { viewModel.deleteProvider(provider.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddCustomProviderDialog(
            onAdd = { name, endpoint, apiKey, models, defaultModel, apiStyle, authHeader, authPrefix ->
                viewModel.addProvider(name, endpoint, apiKey, models, defaultModel, apiStyle, authHeader, authPrefix)
            },
            onDismiss = viewModel::hideDialog
        )
    }

    uiState.editingProvider?.let { editing ->
        EditCustomProviderDialog(
            provider = editing,
            onUpdate = { viewModel.updateProvider(it) },
            onDismiss = viewModel::hideDialog
        )
    }
}

@Composable
private fun CustomProviderCard(
    provider: CustomProvider,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.name, fontWeight = FontWeight.Bold)
                    Text("API 风格: ${provider.apiStyle.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("端点: ${provider.endpoint}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("鉴权头: ${provider.authHeader} (${provider.authPrefix.trim()})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            if (provider.models.isNotEmpty()) {
                Text("模型: ${provider.models.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("编辑") }
                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除自定义平台") },
            text = { Text("确定要删除 \"${provider.name}\" 吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomProviderDialog(
    onAdd: (String, String, String, List<String>, String, ApiStyle, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelsText by remember { mutableStateOf("") }
    var defaultModel by remember { mutableStateOf("") }
    var apiStyle by remember { mutableStateOf(ApiStyle.OPENAI) }
    var authHeader by remember { mutableStateOf("Authorization") }
    var authPrefix by remember { mutableStateOf("Bearer ") }
    var showPassword by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义平台") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState).heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("平台名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("API 端点 *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("https://your-api.com/v1/chat/completions", fontSize = 10.sp) })
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = styleExpanded, onExpandedChange = { styleExpanded = it }) {
                    OutlinedTextField(
                        value = apiStyle.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("API 风格") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                        ApiStyle.values().forEach { style ->
                            DropdownMenuItem(text = { Text("${style.name} - ${style.description()}") }, onClick = { apiStyle = style; styleExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = modelsText, onValueChange = { modelsText = it }, label = { Text("模型列表 (逗号分隔)") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("gpt-4o, gpt-3.5-turbo", fontSize = 10.sp) })
                OutlinedTextField(value = defaultModel, onValueChange = { defaultModel = it }, label = { Text("默认模型") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = authHeader, onValueChange = { authHeader = it }, label = { Text("鉴权头名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = authPrefix, onValueChange = { authPrefix = it }, label = { Text("鉴权前缀") }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Bearer ", fontSize = 10.sp) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val models = modelsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                onAdd(name.trim(), endpoint.trim(), apiKey.trim(), models, defaultModel.trim(), apiStyle, authHeader.trim(), authPrefix.trim())
            }, enabled = name.isNotBlank() && endpoint.isNotBlank()) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCustomProviderDialog(
    provider: CustomProvider,
    onUpdate: (CustomProvider) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(provider.name) }
    var endpoint by remember { mutableStateOf(provider.endpoint) }
    var apiKey by remember { mutableStateOf(provider.apiKey) }
    var modelsText by remember { mutableStateOf(provider.models.joinToString(", ")) }
    var defaultModel by remember { mutableStateOf(provider.defaultModel) }
    var apiStyle by remember { mutableStateOf(provider.apiStyle) }
    var authHeader by remember { mutableStateOf(provider.authHeader) }
    var authPrefix by remember { mutableStateOf(provider.authPrefix) }
    var styleExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑自定义平台") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState).heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("平台名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("API 端点") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = styleExpanded, onExpandedChange = { styleExpanded = it }) {
                    OutlinedTextField(value = apiStyle.name, onValueChange = {}, readOnly = true, label = { Text("API 风格") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                        ApiStyle.values().forEach { style -> DropdownMenuItem(text = { Text("${style.name} - ${style.description()}") }, onClick = { apiStyle = style; styleExpanded = false }) }
                    }
                }
                OutlinedTextField(value = modelsText, onValueChange = { modelsText = it }, label = { Text("模型列表") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = defaultModel, onValueChange = { defaultModel = it }, label = { Text("默认模型") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = authHeader, onValueChange = { authHeader = it }, label = { Text("鉴权头") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = authPrefix, onValueChange = { authPrefix = it }, label = { Text("鉴权前缀") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val models = modelsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                onUpdate(provider.copy(
                    name = name.trim(), endpoint = endpoint.trim(), apiKey = apiKey.trim(),
                    models = models, defaultModel = defaultModel.trim(),
                    apiStyle = apiStyle, authHeader = authHeader.trim(), authPrefix = authPrefix.trim()
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun ApiStyle.description(): String = when (this) {
    ApiStyle.OPENAI -> "OpenAI Chat Completions 兼容（推荐）"
    ApiStyle.ANTHROPIC -> "Anthropic Messages API"
    ApiStyle.GEMINI -> "Google Gemini generateContent"
}
