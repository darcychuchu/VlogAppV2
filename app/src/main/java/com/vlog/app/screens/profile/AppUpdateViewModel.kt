package com.vlog.app.screens.profile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
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
            try {
                val results = appUpdateRepository.checkUpdate()
                if (results != null){
                    _latestVersion.value = results
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasUpdate = hasNewVersion(results),
                        error = null
                    )
                }else{
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasUpdate = false,
                        error = "检查更新失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = e.message ?: "检查更新失败"
                )
            }
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
        val downloadUrl = latestVersion.downloadUrl
        
        Log.d("AppUpdate", "开始下载APK: $downloadUrl")
        
        if (downloadUrl.isNullOrBlank()) {
            Log.e("AppUpdate", "下载地址为空")
            _uiState.value = _uiState.value.copy(
                error = "下载地址无效"
            )
            return
        }
    
        _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
        _downloadProgress.value = 0
    
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // 检查下载目录是否存在
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val fileName = "VlogApp_${latestVersion.versionName ?: "latest"}.apk"
            Log.d("AppUpdate", "下载文件名: $fileName")
            
            // 删除已存在的文件
            val existingFile = File(downloadDir, fileName)
            if (existingFile.exists()) {
                existingFile.delete()
                Log.d("AppUpdate", "删除已存在的文件: ${existingFile.absolutePath}")
            }
            
            val request = DownloadManager.Request(downloadUrl.toUri())
                .setTitle("VlogApp更新")
                .setDescription("正在下载最新版本 v${latestVersion.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setMimeType("application/vnd.android.package-archive")
            
            Log.d("AppUpdate", "下载请求配置完成，URL: $downloadUrl")
            Log.d("AppUpdate", "目标文件: ${downloadDir.absolutePath}/$fileName")
    
            val downloadId = downloadManager.enqueue(request)
            Log.d("AppUpdate", "下载任务已创建，ID: $downloadId")
            
            _uiState.value = _uiState.value.copy(
                downloadId = downloadId,
                isDownloading = true
            )
            
            // 添加进度监听
            monitorDownloadProgress(downloadId, downloadManager)
            
        } catch (e: Exception) {
            Log.e("AppUpdate", "下载失败", e)
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                error = "下载失败: ${e.message}"
            )
        }
    }
    
    // 新增进度监听方法
    private fun monitorDownloadProgress(downloadId: Long, downloadManager: DownloadManager) {
        viewModelScope.launch {
            var downloading = true
            var retryCount = 0
            val maxRetries = 300 // 最多重试5分钟
            
            Log.d("AppUpdate", "开始监听下载进度，ID: $downloadId")
            
            while (downloading && retryCount < maxRetries) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val bytesTotal = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        
                        Log.d("AppUpdate", "下载状态: $status, 已下载: $bytesDownloaded, 总大小: $bytesTotal")
                        
                        // 更新进度
                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                            _downloadProgress.value = progress
                            Log.d("AppUpdate", "下载进度: $progress%")
                        } else if (bytesDownloaded > 0) {
                            // 如果总大小未知，但有下载数据，显示不确定进度
                            val progress = minOf(50 + (retryCount / 6), 90) // 渐进式进度
                            _downloadProgress.value = progress
                            Log.d("AppUpdate", "未知总大小，显示进度: $progress%")
                        }
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                Log.d("AppUpdate", "下载完成")
                                _uiState.value = _uiState.value.copy(
                                    isDownloading = false,
                                    downloadCompleted = true
                                )
                                _downloadProgress.value = 100
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                val reason = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                )
                                Log.e("AppUpdate", "下载失败，错误码: $reason")
                                val errorMsg = when (reason) {
                                    DownloadManager.ERROR_CANNOT_RESUME -> "无法恢复下载"
                                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "存储设备未找到"
                                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
                                    DownloadManager.ERROR_FILE_ERROR -> "文件错误"
                                    DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP数据错误"
                                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
                                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "重定向次数过多"
                                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "未处理的HTTP错误码"
                                    DownloadManager.ERROR_UNKNOWN -> "未知错误"
                                    else -> "下载失败(错误码: $reason)"
                                }
                                _uiState.value = _uiState.value.copy(
                                    isDownloading = false,
                                    error = errorMsg
                                )
                            }
                            DownloadManager.STATUS_PAUSED -> {
                                Log.d("AppUpdate", "下载暂停")
                            }
                            DownloadManager.STATUS_PENDING -> {
                                Log.d("AppUpdate", "下载等待中 - 重试次数: $retryCount")
                                // 检查网络连接状态
                                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
//                                val activeNetwork = connectivityManager.activeNetworkInfo
//                                Log.d("AppUpdate", "网络状态: ${activeNetwork?.isConnected} - ${activeNetwork?.typeName}")
                                
                                // 如果等待时间过长，可能需要重新启动下载
                                if (retryCount > 30) { // 30秒后
                                    Log.w("AppUpdate", "下载等待时间过长，可能存在网络问题")
                                }
                                
                                // 保持0%进度，不要设置固定值
                                _downloadProgress.value = 0
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                Log.d("AppUpdate", "下载进行中")
                                // 只有在实际有下载数据时才更新进度
                                // 进度已在上面的逻辑中处理
                            }
                        }
                    } else {
                        // 查询不到下载任务
                        downloading = false
                        Log.e("AppUpdate", "查询不到下载任务")
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = "下载任务丢失"
                        )
                    }
                    cursor.close()
                } catch (e: Exception) {
                    Log.e("AppUpdate", "查询下载状态异常", e)
                    retryCount++
                }
                
                if (downloading) {
                    kotlinx.coroutines.delay(1000) // 每1秒检查一次进度
                    retryCount++
                }
            }
            
            // 超时处理
            if (retryCount >= maxRetries && downloading) {
                Log.e("AppUpdate", "下载监听超时")
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "下载超时，请检查网络连接"
                )
            }
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