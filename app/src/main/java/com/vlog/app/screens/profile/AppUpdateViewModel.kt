package com.vlog.app.screens.profile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
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

    private var downloadObserver: DownloadProgressObserver? = null
    private val downloadHandler = Handler(Looper.getMainLooper())

    /**
     * 获取当前应用版本信息
     */
    private fun getCurrentVersionInfo(): AppVersionInfo {
        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            val versionName = packageInfo.versionName
            return AppVersionInfo(
                versionCode = versionCode,
                versionName = versionName
            )
        } catch (e: Exception) {
            // Handle exception, perhaps log it or return default values
            Log.e("AppUpdateViewModel", "Error getting package info", e)
            return AppVersionInfo(
                versionCode = 1, // Default or error value
                versionName = APP_VERSION // Default or error value
            )
        }
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
    
    // Refactored to use ContentObserver
    private fun monitorDownloadProgress(downloadId: Long, downloadManager: DownloadManager) {
        downloadObserver?.let { observer ->
            context.contentResolver.unregisterContentObserver(observer)
        }
        downloadObserver = DownloadProgressObserver(downloadHandler, downloadManager, downloadId)

        // The DownloadManager.COLUMN_URI is not directly the URI for the file,
        // but rather a content URI that can be observed.
        // A common practice is to observe the general download URI and filter by ID,
        // or use getUriForDownloadedFile if you only care about the final file state.
        // However, getUriForDownloadedFile might return null if the download hasn't completed.
        // For observing progress, it's better to observe a general URI and filter.
        // The "content://downloads/my_downloads" is a common URI that works for this.
        // If this specific URI doesn't work, "content://downloads/all_downloads" can be an alternative.
        try {
            val downloadUri = Uri.parse("content://downloads/my_downloads") // General URI for downloads
            context.contentResolver.registerContentObserver(
                downloadUri,
                true,
                downloadObserver!!
            )
            Log.d("AppUpdate", "DownloadProgressObserver registered for ID: $downloadId with URI: $downloadUri")
        } catch (e: Exception) {
            Log.e("AppUpdate", "Error registering content observer", e)
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                error = "无法监听下载进度: ${e.message}"
            )
        }
    }

    private inner class DownloadProgressObserver(
        handler: Handler,
        private val downloadManager: DownloadManager,
        private val downloadId: Long
    ) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean) {
            this.onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Filter by downloadId if the URI is general
            if (uri != null && uri.lastPathSegment != null && uri.lastPathSegment != downloadId.toString()) {
                 // If the notification is for a different downloadId, ignore it.
                 // This check is more relevant if observing a very broad URI like "content://downloads/all_downloads".
                 // For "content://downloads/my_downloads", it's often specific enough.
            }

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val totalBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val reasonColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                if (statusColumnIndex == -1 || totalBytesColumnIndex == -1 || downloadedBytesColumnIndex == -1 || reasonColumnIndex == -1) {
                    Log.e("AppUpdate", "Required column not found in DownloadManager query result.")
                    cursor.close()
                    // Potentially unregister observer here if columns are missing, as updates are not possible
                    // unregister()
                    // _uiState.value = _uiState.value.copy(isDownloading = false, error = "无法读取下载信息")
                    return
                }

                val status = cursor.getInt(statusColumnIndex)
                val totalBytes = cursor.getLong(totalBytesColumnIndex)
                val downloadedBytes = cursor.getLong(downloadedBytesColumnIndex)

                Log.d("AppUpdateObserver", "ID $downloadId: Status $status, Bytes $downloadedBytes/$totalBytes")

                if (totalBytes > 0) {
                    _downloadProgress.value = ((downloadedBytes * 100L) / totalBytes).toInt()
                } else if (status == DownloadManager.STATUS_RUNNING && downloadedBytes > 0) {
                     // Indeterminate progress if total size is unknown but bytes are flowing
                    _downloadProgress.value = -1 // Or some other indicator for indeterminate progress
                } else if (status == DownloadManager.STATUS_PENDING) {
                    _downloadProgress.value = 0
                }


                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d("AppUpdateObserver", "ID $downloadId: Download successful")
                        _downloadProgress.value = 100
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadCompleted = true,
                            error = null
                        )
                        unregister()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(reasonColumnIndex)
                        Log.e("AppUpdateObserver", "ID $downloadId: Download failed, reason: $reason")
                        val errorMsg = getDownloadErrorReason(reason)
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadCompleted = false,
                            error = errorMsg
                        )
                        unregister()
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        Log.d("AppUpdateObserver", "ID $downloadId: Download paused")
                        _uiState.value = _uiState.value.copy(isDownloading = true, error = "下载已暂停")
                    }
                    DownloadManager.STATUS_PENDING -> {
                        Log.d("AppUpdateObserver", "ID $downloadId: Download pending")
                         _uiState.value = _uiState.value.copy(isDownloading = true, error = null) // Error handled by timeout if it stays pending too long
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        Log.d("AppUpdateObserver", "ID $downloadId: Download running")
                        _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
                    }
                    else -> {
                        Log.w("AppUpdateObserver", "ID $downloadId: Unknown download status: $status")
                    }
                }
            } else {
                // Cursor is null or empty, this might mean the download was cancelled or removed.
                Log.e("AppUpdateObserver", "ID $downloadId: Download query returned no results. Assuming download cancelled/removed.")
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "下载任务消失或被取消"
                )
                unregister()
            }
            cursor?.close()
        }

        fun unregister() {
            try {
                context.contentResolver.unregisterContentObserver(this)
                Log.d("AppUpdateObserver", "DownloadProgressObserver unregistered for ID: $downloadId")
            } catch (e: Exception) {
                Log.e("AppUpdateObserver", "Error unregistering content observer for ID: $downloadId", e)
            }
            // Nullify the observer in ViewModel to allow GC and prevent re-unregistration
            if (downloadObserver == this) {
                downloadObserver = null
            }
        }
    }

    private fun getDownloadErrorReason(reason: Int): String {
        return when (reason) {
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
    }

    override fun onCleared() {
        super.onCleared()
        downloadObserver?.unregister()
        downloadObserver = null
        Log.d("AppUpdateViewModel", "ViewModel cleared, download observer unregistered.")
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
            downloadId = null // Keep downloadId to potentially retry or reference later if needed
        )
        // Also unregister observer if reset is called while download is in progress
        downloadObserver?.unregister()
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