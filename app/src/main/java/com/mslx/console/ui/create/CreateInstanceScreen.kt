package com.mslx.console.ui.create

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateInstanceScreen(
    onOpenInstances: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConsole: (Long) -> Unit,
    viewModel: CreateInstanceViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHostState.showSnackbar(it) }
    }

    val jarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val (bytes, name) = readFile(context, uri)
                viewModel.uploadCore(bytes, name)
            }
        }
    }
    val packageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val (bytes, name) = readFile(context, uri)
                viewModel.uploadPackage(bytes, name)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("新建实例", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = onOpenInstances, icon = { Icon(Icons.Filled.List, null) }, label = { Text("实例") })
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Filled.Add, null) }, label = { Text("新建") })
                NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("设置") })
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.success -> SuccessContent(
                serverId = state.createdServerId,
                onOpenConsole = { onOpenConsole(state.createdServerId.toLongOrNull() ?: 0L) },
                onReset = viewModel::reset,
                onBackToList = onOpenInstances,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            state.creating -> CreatingContent(
                serverId = state.createdServerId,
                progress = state.creationProgress,
                logs = state.creationLogs,
                onCancel = viewModel::cancelCreation,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> FormContent(
                state = state,
                onUpdate = viewModel::update,
                onModeChange = viewModel::setMode,
                onNext = viewModel::nextStep,
                onPrev = viewModel::prevStep,
                onOpenCoreSelector = viewModel::openCoreSelector,
                onClearCore = viewModel::clearCoreSelection,
                onRemoveUpload = viewModel::removeUploadedCore,
                onPickJar = { jarLauncher.launch("application/java-archive") },
                onPickPackage = { packageLauncher.launch("*/*") },
                onSubmit = viewModel::submit,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }

    if (state.coreSelectorVisible) {
        CoreSelectorDialog(
            categories = state.coreCategories,
            selectedCategoryKey = state.selectedCategoryKey,
            selectedCoreName = state.selectedCoreName,
            versions = state.coreVersions,
            versionDescription = state.coreVersionDescription,
            builds = state.coreBuilds,
            buildsVisible = state.buildsVisible,
            loadingVersions = state.loadingVersions,
            loading = state.coreSelectorLoading,
            onDismiss = viewModel::closeCoreSelector,
            onSelectCategory = viewModel::selectCategory,
            onSelectCoreName = viewModel::selectCoreName,
            onSelectVersion = viewModel::selectVersion,
            onSelectBuild = viewModel::selectBuild,
        )
    }
}

private val MODES = listOf(
    1 to "快速模式",
    2 to "整合包",
    3 to "基岩版",
    4 to "MCDR",
    10 to "自定义",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormContent(
    state: CreateInstanceUiState,
    onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit,
    onModeChange: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenCoreSelector: () -> Unit,
    onClearCore: () -> Unit,
    onRemoveUpload: () -> Unit,
    onPickJar: () -> Unit,
    onPickPackage: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = wizardSteps(state.mode)
    val current = steps.getOrNull(state.step)
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // 模式选择
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MODES.forEach { (value, label) ->
                FilterChip(selected = state.mode == value, onClick = { onModeChange(value) }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(12.dp))
        StepIndicator(steps, state.step)
        Spacer(Modifier.height(12.dp))

        // 切换步骤时内容淡入淡出动画
        androidx.compose.animation.AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 8 })
                    .togetherWith(fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { -it / 8 })
            },
            label = "stepContent",
        ) { _ ->
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (current?.key) {
                    "basic" -> BasicStep(state, onUpdate)
                    "core" -> CoreStep(state, onUpdate, onOpenCoreSelector, onClearCore, onRemoveUpload, onPickJar)
                    "package" -> PackageStep(state, onUpdate, onOpenCoreSelector, onClearCore, onPickPackage)
                    "java" -> JavaStep(state, onUpdate)
                    "mcdr" -> McdrStep(state, onUpdate)
                    "resource" -> ResourceStep(state, onUpdate)
                    "confirm" -> ConfirmStep(state)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.step > 0) {
                OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Text("上一步") }
            }
            if (current?.key == "confirm") {
                Button(onClick = onSubmit, enabled = !state.submitting, modifier = Modifier.weight(1f)) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text(if (state.submitting) "提交中..." else "确认创建", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("下一步") }
            }
        }
    }
}

@Composable
private fun StepIndicator(steps: List<WizardStep>, current: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("步骤 ${current + 1} / ${steps.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(steps.getOrNull(current)?.title.orEmpty(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { (current + 1).toFloat() / steps.size }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BasicStep(state: CreateInstanceUiState, onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit) {
    SectionCard("基本信息") {
        OutlinedTextField(
            value = state.name,
            onValueChange = { v -> onUpdate { it.copy(name = v) } },
            label = { Text("实例名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.path,
            onValueChange = { v -> onUpdate { it.copy(path = v) } },
            label = { Text("实例路径（选填，留空使用默认）") },
            placeholder = { Text("例如: D:\\MyServer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreStep(
    state: CreateInstanceUiState,
    onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit,
    onOpenCoreSelector: () -> Unit,
    onClearCore: () -> Unit,
    onRemoveUpload: () -> Unit,
    onPickJar: () -> Unit,
) {
    SectionCard("服务端核心") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("online" to "在线下载", "manual" to "本地上传", "custom" to "自定义文件名").forEach { (value, label) ->
                FilterChip(selected = state.downloadType == value, onClick = { onUpdate { it.copy(downloadType = value) } }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state.downloadType) {
            "online" -> {
                if (state.core.isNotBlank()) {
                    SelectedCoreCard(state.core, onClearCore)
                } else {
                    OutlinedButton(onClick = onOpenCoreSelector, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Build, null)
                        Text("点击打开服务端核心选择库")
                    }
                }
            }
            "manual" -> UploadRow(
                uploading = state.uploading,
                progress = state.uploadProgress,
                fileName = state.uploadedFileName,
                hasKey = state.coreFileKey.isNotBlank(),
                onPick = onPickJar,
                onRemove = onRemoveUpload,
            )
            else -> OutlinedTextField(
                value = state.core,
                onValueChange = { v -> onUpdate { it.copy(core = v) } },
                label = { Text("核心文件名") },
                placeholder = { Text("例如: server.jar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageStep(
    state: CreateInstanceUiState,
    onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit,
    onOpenCoreSelector: () -> Unit,
    onClearCore: () -> Unit,
    onPickPackage: () -> Unit,
) {
    SectionCard("整合包") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("upload" to "本地上传", "url" to "远程下载", "local" to "本机路径").forEach { (value, label) ->
                FilterChip(selected = state.packageType == value, onClick = { onUpdate { it.copy(packageType = value) } }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state.packageType) {
            "upload" -> UploadRow(
                uploading = state.uploading,
                progress = state.uploadProgress,
                fileName = state.uploadedFileName,
                hasKey = state.packageFileKey.isNotBlank(),
                onPick = onPickPackage,
                onRemove = { onUpdate { it.copy(packageFileKey = "") } },
            )
            "url" -> OutlinedTextField(
                value = state.packageUrl,
                onValueChange = { v -> onUpdate { it.copy(packageUrl = v) } },
                label = { Text("整合包下载地址（.zip/.mrpack）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> OutlinedTextField(
                value = state.packageLocalPath,
                onValueChange = { v -> onUpdate { it.copy(packageLocalPath = v) } },
                label = { Text("本机绝对路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("可选：同时下载服务端核心", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        if (state.core.isNotBlank()) {
            SelectedCoreCard(state.core, onClearCore)
        } else {
            OutlinedButton(onClick = onOpenCoreSelector, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Build, null)
                Text("选择服务端核心")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JavaStep(state: CreateInstanceUiState, onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit) {
    SectionCard("Java 环境") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("online" to "在线下载", "local" to "电脑上的 Java", "env" to "环境变量", "custom" to "自定义路径", "docker" to "Docker").forEach { (value, label) ->
                FilterChip(selected = state.javaType == value, onClick = { onUpdate { it.copy(javaType = value) } }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state.javaType) {
            "online" -> {
                Text("Java 版本", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.onlineJavaVersions.forEach { version ->
                        FilterChip(selected = state.selectedJavaVersion == version, onClick = { onUpdate { it.copy(selectedJavaVersion = version) } }, label = { Text("Java $version") })
                    }
                }
            }
            "local" -> {
                Text("选择已安装的 Java", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.localJavas.forEach { java ->
                        val label = "Java ${java.version} (${java.vendor ?: "未知"})"
                        FilterChip(selected = state.customJavaPath == java.path, onClick = { onUpdate { it.copy(customJavaPath = java.path) } }, label = { Text(label) })
                    }
                }
            }
            "env" -> Text("将使用系统环境变量中的 java 命令", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            "custom" -> OutlinedTextField(
                value = state.customJavaPath,
                onValueChange = { v -> onUpdate { it.copy(customJavaPath = v) } },
                label = { Text("Java 可执行文件路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            "docker" -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("preset" to "MSLX 官方镜像", "custom" to "自定义镜像").forEach { (value, label) ->
                        FilterChip(selected = state.dockerImageType == value, onClick = { onUpdate { it.copy(dockerImageType = value) } }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.dockerImageType == "preset") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("25", "21", "17", "11", "8").forEach { version ->
                            FilterChip(selected = state.dockerImagePresetVersion == version, onClick = { onUpdate { it.copy(dockerImagePresetVersion = version) } }, label = { Text("Java $version") })
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state.dockerCustomImage,
                        onValueChange = { v -> onUpdate { it.copy(dockerCustomImage = v) } },
                        label = { Text("自定义镜像") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun McdrStep(state: CreateInstanceUiState, onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit) {
    SectionCard("MCDR 配置") {
        OutlinedTextField(
            value = state.mcdrPython,
            onValueChange = { v -> onUpdate { it.copy(mcdrPython = v) } },
            label = { Text("Python 可执行文件") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.mcdrHandler,
            onValueChange = { v -> onUpdate { it.copy(mcdrHandler = v) } },
            label = { Text("Handler（选填，自动推断）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.mcdrInstall, onCheckedChange = { v -> onUpdate { it.copy(mcdrInstall = v) } })
            Text("自动安装 MCDReforged", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ResourceStep(state: CreateInstanceUiState, onUpdate: ((CreateInstanceUiState) -> CreateInstanceUiState) -> Unit) {
    var minUnit by remember { mutableStateOf("GB") }
    var maxUnit by remember { mutableStateOf("GB") }
    SectionCard("资源配置") {
        MemoryField("最小内存", state.minM, minUnit, { minUnit = it }, { mb -> onUpdate { it.copy(minM = mb) } })
        Spacer(Modifier.height(8.dp))
        MemoryField("最大内存", state.maxM, maxUnit, { maxUnit = it }, { mb -> onUpdate { it.copy(maxM = mb) } })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.args,
            onValueChange = { v -> onUpdate { it.copy(args = v) } },
            label = { Text("JVM 启动参数（选填）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.ignoreEula, onCheckedChange = { v -> onUpdate { it.copy(ignoreEula = v) } })
            Text("自动同意 EULA", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ConfirmStep(state: CreateInstanceUiState) {
    SectionCard("确认信息") {
        SummaryRow("实例名称", state.name)
        SummaryRow("实例路径", state.path.ifBlank { "默认路径" })
        if (state.mode == 3) {
            SummaryRow("服务端核心", state.core.ifBlank { "基岩版核心" })
        } else {
            SummaryRow("服务端核心", state.core)
        }
        if (state.mode == 2) {
            val pkg = when {
                state.packageFileKey.isNotBlank() -> "已上传整合包"
                state.packageUrl.isNotBlank() -> "远程下载：${state.packageUrl}"
                state.packageLocalPath.isNotBlank() -> "本机路径：${state.packageLocalPath}"
                else -> "未配置"
            }
            SummaryRow("整合包", pkg)
        }
        if (state.mode != 3) {
            SummaryRow("Java 环境", javaDisplay(state))
        }
        if (state.mode == 4) {
            SummaryRow("MCDR Python", state.mcdrPython)
        }
        SummaryRow("最小内存", "${state.minM} MB")
        SummaryRow("最大内存", "${state.maxM} MB")
        SummaryRow("EULA", if (state.ignoreEula) "自动同意" else "手动同意")
        if (state.args.isNotBlank()) {
            SummaryRow("启动参数", state.args)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun javaDisplay(state: CreateInstanceUiState): String = when (state.javaType) {
    "online" -> "在线下载 Java ${state.selectedJavaVersion}"
    "local", "custom" -> state.customJavaPath.ifBlank { "未指定路径" }
    "env" -> "环境变量 (java)"
    "docker" -> if (state.dockerImageType == "preset") "Docker 镜像 Java ${state.dockerImagePresetVersion}" else state.dockerCustomImage.ifBlank { "Docker 自定义镜像" }
    else -> "未配置"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadRow(
    uploading: Boolean,
    progress: Int,
    fileName: String,
    hasKey: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    if (uploading) {
        Column(Modifier.fillMaxWidth()) {
            Text("正在上传: $fileName", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
    } else if (hasKey) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fileName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "移除") }
        }
    } else {
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null)
            Text("选择文件并上传")
        }
    }
}

@Composable
private fun SelectedCoreCard(core: String, onClear: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(core, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClear) { Icon(Icons.Filled.Delete, "移除") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoryField(
    label: String,
    valueMb: Int,
    unit: String,
    onUnitChange: (String) -> Unit,
    onValueChange: (Int) -> Unit,
) {
    val display = if (unit == "GB") (valueMb / 1024f).let { if (it % 1f == 0f) it.toInt().toString() else String.format("%.1f", it) } else valueMb.toString()
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = display,
                onValueChange = { text ->
                    val num = text.toFloatOrNull() ?: return@OutlinedTextField
                    onValueChange(if (unit == "GB") (num * 1024).toInt().coerceAtLeast(1) else num.toInt().coerceAtLeast(1))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            FlowRow(modifier = Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("GB", "MB").forEach { u ->
                    FilterChip(selected = unit == u, onClick = { onUnitChange(u) }, label = { Text(u) })
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CreatingContent(
    serverId: String,
    progress: Double,
    logs: List<CreationLog>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 进度条平滑前进动画
    val animatedProgress by animateFloatAsState(
        targetValue = (progress / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "creationProgress",
    )
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("正在创建实例 ($serverId)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("${progress.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            logs.takeLast(20).forEach { log ->
                Text(log.message, style = MaterialTheme.typography.bodySmall, color = if (log.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onCancel) { Text("取消创建") }
    }
}

@Composable
private fun SuccessContent(
    serverId: String,
    onOpenConsole: () -> Unit,
    onReset: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🎉 创建成功", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("服务器 ($serverId) 已创建成功", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenConsole, modifier = Modifier.fillMaxWidth()) { Text("进入控制台") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToList, modifier = Modifier.fillMaxWidth()) { Text("返回实例列表") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onReset) { Text("继续创建新实例") }
    }
}

private suspend fun readFile(context: Context, uri: Uri): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
    var name = "file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx) ?: name
        }
    }
    bytes to name
}
