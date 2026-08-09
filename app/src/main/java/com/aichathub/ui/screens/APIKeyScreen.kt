package com.aichathub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichathub.domain.model.AIPlatform
import com.aichathub.domain.model.APIKeyInfo
import com.aichathub.ui.components.getPlatformIcon
import com.aichathub.ui.viewmodel.APIKeyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeyScreen(
    onBack: () -> Unit,
    onNavigateToCustomProviders: () -> Unit,
    viewModel: APIKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.testResult) {
        uiState.testResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearTestResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key 管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    TextButton(onClick = onNavigateToCustomProviders) {
                        Icon(Icons.Filled.Extension, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("自定义平台")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        if (uiState.apiKeys.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("尚无 API Key", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("点击右下角 + 添加", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.apiKeys, key = { it.id }) { key ->
                    APIKeyCard(
                        key = key,
                        isTesting = uiState.isTesting && uiState.testResultKeyId == key.id,
                        onSetActive = { viewModel.setActiveAPIKey(key.id) },
                        onTest = { viewModel.testConnection(key) },
                        onEdit = { viewModel.showEditDialog(key) },
                        onDelete = { viewModel.deleteAPIKey(key.id) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddAPIKeyDialog(
            onAdd = { platform, apiKey, name, endpoint, customModels, customModelOverride ->
                viewModel.addAPIKey(platform, apiKey, name, endpoint, customModels, customModelOverride)
            },
            onDismiss = viewModel::hideAddDialog
        )
    }

    uiState.editingKey?.let { editingKey ->
        EditAPIKeyDialog(
            key = editingKey,
            onUpdate = { newApiKey, newName, newEndpoint, newCustomModels, newModelOverride ->
                viewModel.updateAPIKey(editingKey, newApiKey, newName, newEndpoint, newCustomModels, newModelOverride)
            },
            onDismiss = viewModel::hideEditDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun APIKeyCard(
    key: APIKeyInfo,
    isTesting: Boolean,
    onSetActive: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(getPlatformIcon(key.platform), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(key.name, fontWeight = FontWeight.Bold)
                    Text(key.platform.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (key.isActive) {
                    AssistChip(onClick = {}, label = { Text("活跃", fontSize = 10.sp) }, leadingIcon = { Icon(Icons.Filled.Check, null, modifier = Modifier.size(12.dp)) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 模型信息
            val models = key.availableModels()
            if (models.isNotEmpty()) {
                Text("可用模型: ${models.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            if (key.customEndpoint != null) {
                Text("自定义端点: ${key.customEndpoint}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Text("默认模型: ${key.defaultModel()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!key.isActive) {
                    OutlinedButton(onClick = onSetActive) { Text("设为活跃") }
                }
                Button(onClick = onTest, enabled = !isTesting) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("测试中…")
                    } else {
                        Text("测试连接")
                    }
                }
                OutlinedButton(onClick = onEdit) { Text("编辑") }
                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除 API Key") },
            text = { Text("确定要删除 \"${key.name}\" 吗？") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAPIKeyDialog(
    onAdd: (AIPlatform, String, String, String?, List<String>, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(AIPlatform.DEEPSEEK) }
    var apiKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var customEndpoint by remember { mutableStateOf("") }
    var customModelsText by remember { mutableStateOf("") }
    var modelOverride by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 API Key") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState).heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedPlatform.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("平台") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        leadingIcon = { Icon(getPlatformIcon(selectedPlatform), null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AIPlatform.builtIn.forEach { p ->
                            DropdownMenuItem(text = { Text(p.displayName) }, onClick = { selectedPlatform = p; expanded = false }, leadingIcon = { Icon(getPlatformIcon(p), null) })
                        }
                    }
                }
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customEndpoint,
                    onValueChange = { customEndpoint = it },
                    label = { Text("自定义端点 (可选)") },
                    placeholder = { Text(selectedPlatform.defaultEndpoint, fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelOverride,
                    onValueChange = { modelOverride = it },
                    label = { Text("自定义模型名 (可选)") },
                    placeholder = { Text(selectedPlatform.defaultModel) },
                    singleLine = true,
                    supportingText = { Text("留空则使用默认模型", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customModelsText,
                    onValueChange = { customModelsText = it },
                    label = { Text("额外可选模型 (可选)") },
                    placeholder = { Text("用逗号分隔，例如: gpt-4o, gpt-4-vision") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val models = customModelsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    val endpoint = customEndpoint.ifBlank { null }
                    val override = modelOverride.ifBlank { null }
                    onAdd(selectedPlatform, apiKey.trim(), name.trim(), endpoint, models, override)
                },
                enabled = apiKey.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAPIKeyDialog(
    key: APIKeyInfo,
    onUpdate: (String?, String?, String?, List<String>?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(key.name) }
    var customEndpoint by remember { mutableStateOf(key.customEndpoint ?: "") }
    var customModelsText by remember { mutableStateOf(key.customModels.joinToString(", ")) }
    var modelOverride by remember { mutableStateOf(key.customModelOverride ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 API Key") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState).heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("平台: ${key.platform.displayName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("新 API Key (留空保留原值)") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customEndpoint,
                    onValueChange = { customEndpoint = it },
                    label = { Text("自定义端点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelOverride,
                    onValueChange = { modelOverride = it },
                    label = { Text("自定义模型名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customModelsText,
                    onValueChange = { customModelsText = it },
                    label = { Text("额外可选模型") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newKey = apiKey.ifBlank { null }
                val newName = if (name != key.name) name else null
                val newEndpoint = if (customEndpoint != (key.customEndpoint ?: "")) customEndpoint.ifBlank { null } else null
                val newModels = if (customModelsText != key.customModels.joinToString(", ")) {
                    customModelsText.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                } else null
                val newOverride = if (modelOverride != (key.customModelOverride ?: "")) modelOverride.ifBlank { null } else null
                onUpdate(newKey, newName, newEndpoint, newModels, newOverride)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
