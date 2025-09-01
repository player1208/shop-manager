package com.example.storemanagerassitent.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.storemanagerassitent.utils.CrashReporter
import kotlinx.coroutines.launch
import com.example.storemanagerassitent.data.db.ServiceLocator
import com.example.storemanagerassitent.data.DataStoreManager

/**
 * 调试屏幕 - 用于查看崩溃日志和错误信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    dataStoreManager: DataStoreManager? = null
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var crashLogs by remember { mutableStateOf("") }
    var errorLogs by remember { mutableStateOf("") }
    var logFileInfo by remember { mutableStateOf("") }
    
    // 加载日志数据
    LaunchedEffect(Unit) {
        loadLogs { crash, error, info ->
            crashLogs = crash
            errorLogs = error
            logFileInfo = info
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试日志") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            loadLogs { crash, error, info ->
                                crashLogs = crash
                                errorLogs = error
                                logFileInfo = info
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            // 清空 Room 全部数据
                            ServiceLocator.adminRepository.clearAll()
                            // 清空 DataStore 数据并禁用下次种子写入
                            dataStoreManager?.let { ds ->
                                ds.clearAllData()
                                val settings = ds.getAppSettings()
                                ds.saveAppSettings(settings.copy(seedDisabled = true))
                            }
                            // 清理日志
                            CrashReporter.clearLogs()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空测试数据")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 日志文件信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "日志文件信息",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = logFileInfo,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            // Tab选择器
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ) {
                    Text(
                        text = "崩溃日志",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ) {
                    Text(
                        text = "错误日志",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // 日志内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> LogContent(
                        title = "崩溃日志",
                        content = crashLogs
                    )
                    1 -> LogContent(
                        title = "错误日志",
                        content = errorLogs
                    )
                }
            }
        }
    }
}

@Composable
private fun LogContent(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (content.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无日志",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

private fun loadLogs(onLoaded: (String, String, String) -> Unit) {
    val crashLogs = CrashReporter.getCrashLogs()
    val errorLogs = CrashReporter.getErrorLogs()
    val logFileInfo = CrashReporter.getLogFileInfo()
    onLoaded(crashLogs, errorLogs, logFileInfo)
}
