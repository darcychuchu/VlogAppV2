package com.vlog.app.screens.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentVersion by viewModel.currentVersion.collectAsState()
    val latestVersion by viewModel.latestVersion.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "版本更新",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前版本信息
            CurrentVersionCard(currentVersion = currentVersion)

            // 最新版本信息
            latestVersion?.let { latest ->
                LatestVersionCard(
                    latestVersion = latest,
                    hasUpdate = uiState.hasUpdate
                )
            }

            // 更新状态和操作按钮
            UpdateActionSection(
                uiState = uiState,
                onCheckUpdate = { viewModel.checkForUpdate() },
                onDownload = { viewModel.downloadApk() },
                onInstall = { viewModel.installApk() },
                onClearError = { viewModel.clearError() }
            )

            // 下载进度
            if (uiState.isDownloading) {
                DownloadProgressSection(progress = downloadProgress)
            }
        }
    }
}

@Composable
fun CurrentVersionCard(currentVersion: AppVersionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "当前版本",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "版本号: ${currentVersion.versionCode}",
                    fontSize = 14.sp
                )
                Text(
                    text = "版本名: ${currentVersion.versionName}",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LatestVersionCard(
    latestVersion: com.vlog.app.data.versions.AppVersion,
    hasUpdate: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUpdate) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最新版本",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasUpdate) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (hasUpdate) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = "有更新",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "版本号: ${latestVersion.versionCode}",
                    fontSize = 14.sp
                )
                Text(
                    text = "版本名: ${latestVersion.versionName ?: "未知"}",
                    fontSize = 14.sp
                )
            }
            
            latestVersion.description?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "更新说明:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (latestVersion.fileSize > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "文件大小: ${formatFileSize(latestVersion.fileSize)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UpdateActionSection(
    uiState: AppUpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onClearError: () -> Unit
) {
    Column {
        // 错误信息
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text("关闭")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 检查更新按钮
            Button(
                onClick = onCheckUpdate,
                enabled = !uiState.isChecking && !uiState.isDownloading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("检查更新")
            }

            // 下载/安装按钮
            if (uiState.hasUpdate) {
                Button(
                    onClick = if (uiState.downloadCompleted) onInstall else onDownload,
                    enabled = !uiState.isChecking && !uiState.isDownloading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = if (uiState.downloadCompleted) Icons.Default.Update else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.downloadCompleted) "安装" else "下载")
                }
            }
        }
    }
}

@Composable
fun DownloadProgressSection(progress: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "下载进度",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$progress%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 格式化文件大小
 */
@SuppressLint("DefaultLocale")
fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}