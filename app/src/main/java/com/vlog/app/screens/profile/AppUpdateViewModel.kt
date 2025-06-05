package com.vlog.app.screens.profile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.versions.AppUpdateRepository
import com.vlog.app.data.versions.AppVersion
import com.vlog.app.di.Constants.APP_VERSION
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val _currentVersion = MutableStateFlow(getCurrentVersionInfo())
    val currentVersion: StateFlow<AppVersionInfo> = _currentVersion.asStateFlow()

    private val _latestVersion = MutableStateFlow<AppVersion?>(null)
    val latestVersion: StateFlow<AppVersion?> = _latestVersion.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    /**
     * 获取当前应用版本信息
     */
    private fun getCurrentVersionInfo(): AppVersionInfo {
        return AppVersionInfo(
            versionCode = 1,
            versionName = APP_VERSION
        )
    }

    /**
     * 检查应用更新
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null)
            
            appUpdateRepository.checkUpdate().fold(
                onSuccess = { appVersion ->
                    _latestVersion.value = appVersion
                    val hasUpdate = hasNewVersion(appVersion)
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasUpdate = hasUpdate,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        error = exception.message ?: "检查更新失败"
                    )
                }
            )
        }
    }

    /**
     * 判断是否有新版本
     */
    private fun hasNewVersion(latestVersion: AppVersion): Boolean {
        val currentVersionCode = 1
        val currentVersionName = APP_VERSION

        return latestVersion.versionCode > currentVersionCode ||
               latestVersion.versionName != currentVersionName
    }

    /**
     * 下载APK文件
     */
    fun downloadApk() {
        val latestVersion = _latestVersion.value ?: return
        val downloadUrl = latestVersion.downloadUrl ?: return

        _uiState.value = _uiState.value.copy(isDownloading = true)

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(downloadUrl.toUri())
                .setTitle("VlogApp更新")
                .setDescription("正在下载最新版本")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VlogApp_${latestVersion.versionName}.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)
            
            // 监听下载进度（简化版本，实际项目中可能需要更复杂的进度监听）
            _uiState.value = _uiState.value.copy(
                downloadId = downloadId,
                isDownloading = true
            )
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                error = "下载失败: ${e.message}"
            )
        }
    }

    /**
     * 安装APK
     */
    fun installApk() {
        val latestVersion = _latestVersion.value ?: return
        
        try {
            val apkFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "VlogApp_${latestVersion.versionName}.apk"
            )
            
            if (apkFile.exists()) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                context.startActivity(installIntent)
                
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadCompleted = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "APK文件不存在，请重新下载"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "安装失败: ${e.message}"
            )
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 重置下载状态
     */
    fun resetDownloadState() {
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadCompleted = false,
            downloadId = null
        )
    }
}

/**
 * 应用更新UI状态
 */
data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadCompleted: Boolean = false,
    val downloadId: Long? = null,
    val error: String? = null
)

/**
 * 当前应用版本信息
 */
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String
)