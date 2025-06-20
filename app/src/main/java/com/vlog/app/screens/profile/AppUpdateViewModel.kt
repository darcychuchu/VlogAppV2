package com.vlog.app.screens.profile

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
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

    companion object {
        const val TAG = "AppUpdateVM" // Removed private modifier
    }

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val _currentVersion = MutableStateFlow(getCurrentVersionInfo())
    val currentVersion: StateFlow<AppVersionInfo> = _currentVersion.asStateFlow()

    private val _latestVersion = MutableStateFlow<AppVersion?>(null)
    val latestVersion: StateFlow<AppVersion?> = _latestVersion.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private var downloadObserver: DownloadProgressObserver? = null
    private var downloadReceiver: DownloadCompletionReceiver? = null
    private val downloadHandler = Handler(Looper.getMainLooper())
    private var currentDownloadId: Long? = null

    init {
        Log.d(TAG, "ViewModel initialized.")
        Log.d(TAG, "Initial current version: ${currentVersion.value}")
    }

    /**
     * 获取当前应用版本信息
     */
    private fun getCurrentVersionInfo(): AppVersionInfo {
        Log.d(TAG, "getCurrentVersionInfo called")
        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode =
                packageInfo.longVersionCode.toInt()
            val versionName = packageInfo.versionName
            val info = AppVersionInfo(
                versionCode = versionCode,
                versionName = versionName!!
            )
            Log.d(TAG, "Current version info retrieved: $info")
            return info
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package info", e)
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
        Log.d(TAG, "checkForUpdate called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null)
            try {
                val results = appUpdateRepository.checkUpdate()
                if (results != null){
                    _latestVersion.value = results
                    val hasUpdate = hasNewVersion(results)
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasUpdate = hasUpdate,
                        error = null
                    )
                    Log.d(TAG, "Update check successful. Latest version: $results, Has update: $hasUpdate")
                }else{
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasUpdate = false,
                        error = "检查更新失败"
                    )
                    Log.w(TAG, "Update check returned null results.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed with exception", e)
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
        val currentVersionCode = currentVersion.value.versionCode
        val currentVersionName = currentVersion.value.versionName
        return latestVersion.versionCode > currentVersionCode && latestVersion.versionName != currentVersionName
    }

    /**
     * 下载APK文件
     */
    fun downloadApk() {
        Log.d(TAG, "downloadApk called")
        // Reset permission flags first, in case they were true before
        _uiState.value = _uiState.value.copy(
            requiresStoragePermission = false,
            requiresInstallPermission = false,
            error = null // Clear previous permission related error message
        )
        Log.d(TAG, "Permission flags reset in UI state.")

        val latestVersion = _latestVersion.value
        if (latestVersion == null) {
            Log.e(TAG, "Latest version is null, cannot start download.")
            _uiState.value = _uiState.value.copy(error = "无法获取最新版本信息，请稍后重试。")
            return
        }
        val downloadUrl = latestVersion.downloadUrl
        
        Log.d(TAG, "Starting APK download from URL: $downloadUrl")
        
        if (downloadUrl.isNullOrBlank()) {
            Log.e(TAG, "Download URL is null or blank.")
            _uiState.value = _uiState.value.copy(
                error = "下载地址无效"
            )
            return
        }
    
        _uiState.value = _uiState.value.copy(isDownloading = true, error = null, requiresStoragePermission = false)
        Log.d(TAG, "UI state updated: isDownloading=true, requiresStoragePermission=false (if previously true and now resolved)")
        _downloadProgress.value = 0
    
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
                Log.d(TAG, "Download directory created: ${downloadDir.absolutePath}")
            }
            
            val fileName = "VlogApp_${latestVersion.versionName ?: "latest"}.apk"
            Log.d(TAG, "Target filename: $fileName")
            
            val existingFile = File(downloadDir, fileName)
            if (existingFile.exists()) {
                existingFile.delete()
                Log.d(TAG, "Deleted existing file: ${existingFile.absolutePath}")
            }
            
            val request = DownloadManager.Request(downloadUrl.toUri())
            request.setTitle("VlogApp更新")
            request.setDescription("正在下载最新版本 v${latestVersion.versionName}")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
            request.setMimeType("application/vnd.android.package-archive")
            
            //Log.d(TAG, "DownloadManager.Request configured: Title=${request.getTitle()}, Description=${request.getDescription()}, Destination=${Environment.DIRECTORY_DOWNLOADS}/$fileName")
    
            val downloadId = downloadManager.enqueue(request)
            currentDownloadId = downloadId
            Log.i(TAG, "Download enqueued. Download ID: $downloadId")
            
            _uiState.value = _uiState.value.copy(
                downloadId = downloadId,
                isDownloading = true
            )
            Log.d(TAG, "UI state updated with downloadId and isDownloading=true.")
            
            monitorDownload(downloadId, downloadManager)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed with exception", e)
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                error = "下载失败: ${e.message}"
            )
        }
    }
    
    // Setup both ContentObserver and BroadcastReceiver
    private fun monitorDownload(downloadId: Long, downloadManager: DownloadManager) {
        Log.i(TAG, "monitorDownload: (Re-)Registering listeners for download ID: $downloadId")
        // Register ContentObserver for progress
        downloadObserver?.unregister()
        downloadObserver = DownloadProgressObserver(downloadHandler, downloadManager, downloadId)
        val observerUri = "content://downloads/my_downloads".toUri()
        try {
            context.contentResolver.registerContentObserver(
                observerUri,
                true,
                downloadObserver!!
            )
            Log.d(TAG, "DownloadProgressObserver registered for ID: $downloadId, URI: $observerUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering DownloadProgressObserver for ID: $downloadId", e)
        }

        // Register BroadcastReceiver for completion
        downloadReceiver?.unregister()
        downloadReceiver = DownloadCompletionReceiver()
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        try {
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "DownloadCompletionReceiver registered for ID: $downloadId with filter: ${intentFilter.getAction(0)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering DownloadCompletionReceiver for ID: $downloadId", e)
             _uiState.value = _uiState.value.copy(
                isDownloading = false,
                error = "无法监听下载完成状态: ${e.message}"
            )
        }
    }

    internal inner class DownloadCompletionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "DownloadCompletionReceiver onReceive called! Intent: $intent")
            Log.i(TAG, "DownloadCompletionReceiver: Intent action: ${intent?.action}")
            
            val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            Log.d(TAG, "DownloadCompletionReceiver onReceive: receivedId=$receivedId, currentDownloadId=$currentDownloadId")

            if (receivedId == -1L || receivedId != currentDownloadId) {
                Log.d(TAG, "DownloadCompletionReceiver: Ignoring broadcast for unrelated ID ($receivedId)")
                return // Not our download
            }

            Log.i(TAG, "DownloadCompletionReceiver: Handling completion for ID: $receivedId")
            val dm = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                Log.e(TAG, "DownloadCompletionReceiver: DownloadManager service not available.")
                return
            }
            val query = DownloadManager.Query().setFilterById(receivedId!!)
            val cursor = dm.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                var status = -1 // Default to an invalid status

                if (statusColumnIndex != -1) {
                    status = cursor.getInt(statusColumnIndex)
                    Log.i(TAG, "DownloadCompletionReceiver: Download status for ID $receivedId is: $status")
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        if (_uiState.value.error == "下载已暂停") { // Check for the exact error message
                            Log.i(TAG, "Receiver: Download successful (was previously paused). Forcing state update and install.")
                        }
                        Log.d(TAG, "Receiver: Download successful. Updating UI state: isDownloading=false, downloadCompleted=true")
                        _downloadProgress.value = 100
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadCompleted = true,
                            error = null
                        )
                        Log.i(TAG, "DownloadCompletionReceiver: About to call installApk() for ID $receivedId")
                        installApk()
                        Log.d(TAG, "Receiver: -----------------------------------------------------UI state updated. Current isDownloading: ${_uiState.value.isDownloading}, downloadCompleted: ${_uiState.value.downloadCompleted}")
                        Log.i(TAG, "DownloadCompletionReceiver: ------------------------------------------------ID $receivedId SUCCESSFUL. UI updated. Attempting install.")

                    } else if (status == DownloadManager.STATUS_FAILED) {
                        val reason = if (reasonColumnIndex != -1) cursor.getInt(reasonColumnIndex) else -1
                        val errorMsg = getDownloadErrorReason(reason)
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadCompleted = false,
                            error = errorMsg
                        )
                        Log.e(TAG, "DownloadCompletionReceiver: ID $receivedId FAILED. Reason: $reason, ErrorMsg: $errorMsg. UI updated.")
                    } else {
                        Log.w(TAG, "DownloadCompletionReceiver: ID $receivedId completed with unexpected status: $status")
                    }
                } else {
                     Log.e(TAG, "DownloadCompletionReceiver: Could not find status column for ID $receivedId")
                }
                cursor.close()
                Log.i(TAG, "Receiver: Download processing finished for ID: $receivedId (Reported Status: $status). Calling unregisterListeners.")
            } else {
                 Log.e(TAG, "DownloadCompletionReceiver: Cursor null or empty for ID $receivedId")
                 Log.i(TAG, "Receiver: Download processing finished due to empty cursor for ID: $receivedId. Calling unregisterListeners.")
            }
            unregisterListeners()
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
            Log.d(TAG, "DownloadProgressObserver onChange: selfChange=$selfChange, uri=$uri, for downloadId=$downloadId")

            // Filter by downloadId if the URI is general - this part is tricky as observed URI might not directly contain ID
            // For "content://downloads/my_downloads", it's often necessary to query all active downloads or filter by the known ID.
            // The current implementation directly queries by downloadId, which is correct.

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val totalBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val reasonColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                if (statusColumnIndex == -1 || totalBytesColumnIndex == -1 || downloadedBytesColumnIndex == -1 || reasonColumnIndex == -1) {
                    Log.e(TAG, "DownloadProgressObserver: Required column not found in DownloadManager query result for ID $downloadId.")
                    cursor.close()
                    return
                }

                val status = cursor.getInt(statusColumnIndex)
                val totalBytes = cursor.getLong(totalBytesColumnIndex)
                val downloadedBytes = cursor.getLong(downloadedBytesColumnIndex)

                val currentProgress = if (totalBytes > 0) {
                    ((downloadedBytes * 100L) / totalBytes).toInt()
                } else if (status == DownloadManager.STATUS_RUNNING && downloadedBytes > 0) {
                    -1 // Indeterminate
                } else if (status == DownloadManager.STATUS_PENDING) {
                    0
                } else {
                    _downloadProgress.value // Keep current if no better info
                }
                _downloadProgress.value = currentProgress

                Log.d(TAG, "DownloadProgressObserver: ID $downloadId, Status $status, Bytes $downloadedBytes/$totalBytes, Progress $currentProgress%")

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d(TAG, "DownloadProgressObserver: ID $downloadId SUCCESSFUL. Will be handled by BroadcastReceiver.")
                        // Update UI state and call installApk as backup in case BroadcastReceiver doesn't fire
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadCompleted = true,
                            error = null
                        )
                        Log.i(TAG, "DownloadProgressObserver: Calling installApk() as backup for ID $downloadId")
                        installApk()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Log.d(TAG, "DownloadProgressObserver: ID $downloadId FAILED (event handled by Receiver).")
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        Log.d(TAG, "DownloadProgressObserver: ID $downloadId PAUSED.")
                        if (_uiState.value.downloadId == downloadId && !_uiState.value.downloadCompleted) {
                           _uiState.value = _uiState.value.copy(isDownloading = true, error = "下载已暂停")
                           Log.d(TAG, "DownloadProgressObserver: UI state updated for PAUSED.")
                           Log.d(TAG, "DownloadProgressObserver: PAUSED state. isDownloading is now: ${_uiState.value.isDownloading}")
                        }
                    }
                    DownloadManager.STATUS_PENDING -> {
                        Log.d(TAG, "DownloadProgressObserver: ID $downloadId PENDING.")
                         if (_uiState.value.downloadId == downloadId && !_uiState.value.downloadCompleted) {
                            _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
                            Log.d(TAG, "DownloadProgressObserver: UI state updated for PENDING.")
                         }
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        Log.d(TAG, "DownloadProgressObserver: ID $downloadId RUNNING.")
                         if (_uiState.value.downloadId == downloadId && !_uiState.value.downloadCompleted) {
                            _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
                            // Log.d(TAG, "DownloadProgressObserver: UI state updated for RUNNING.") // Can be too noisy
                         }
                    }
                    else -> {
                        Log.w(TAG, "DownloadProgressObserver: ID $downloadId Unknown download status: $status")
                    }
                }
            } else {
                Log.w(TAG, "DownloadProgressObserver: ID $downloadId query returned no results. May have been cancelled/removed.")
            }
            cursor?.close()
        }

        fun unregister() {
            if (downloadObserver == null) {
                Log.d(TAG, "DownloadProgressObserver for ID $downloadId already unregistered or never registered.")
                return
            }
            try {
                context.contentResolver.unregisterContentObserver(this)
                Log.i(TAG, "DownloadProgressObserver unregistered for ID: $downloadId")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering DownloadProgressObserver for ID: $downloadId", e)
            }
            downloadObserver = null
        }
    }

    private fun unregisterListeners() {
        Log.i(TAG, "unregisterListeners: Attempting to unregister observer and receiver.")
        if (downloadObserver != null) {
            Log.d(TAG, "unregisterListeners: Unregistering DownloadProgressObserver.")
            downloadObserver?.unregister()
        } else {
            Log.d(TAG, "unregisterListeners: DownloadProgressObserver was already null.")
        }
        if (downloadReceiver != null) {
            Log.d(TAG, "unregisterListeners: Unregistering DownloadCompletionReceiver.")
            downloadReceiver?.unregister()
        } else {
            Log.d(TAG, "unregisterListeners: DownloadCompletionReceiver was already null.")
        }
    }
    private fun getDownloadErrorReason(reason: Int): String {
        // No specific logging here as it's a pure helper, logging happens at call site.
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
        Log.i(TAG, "onCleared: Calling unregisterListeners to clean up.")
        unregisterListeners()
        currentDownloadId = null
    }

    /**
     * 安装APK
     */
    fun installApk() {
        // Log entry point with context if possible (e.g. called by receiver)
        // For now, a general entry log. The call from receiver is already logged in the receiver.
        Log.i(TAG, "installApk() called. Current download ID: $currentDownloadId")

        _uiState.value = _uiState.value.copy(requiresInstallPermission = false, error = null)
        Log.d(TAG, "Install permission flag reset in UI state.")

        // Log current state of 'canRequestPackageInstalls' before the check
        val canRequestPackageInstalls =
            context.packageManager.canRequestPackageInstalls()
        Log.d(TAG, "installApk: Checking 'canRequestPackageInstalls' permission. Currently granted: $canRequestPackageInstalls")

        if (!canRequestPackageInstalls) { // Use the pre-fetched value
            Log.w(TAG, "REQUEST_INSTALL_PACKAGES permission not granted.")
            _uiState.value = _uiState.value.copy(
                requiresInstallPermission = true,
                error = "安装未知应用权限未授予，无法安装更新。"
            )
            Log.d(TAG, "UI state updated: requiresInstallPermission=true")
            return
        }
        Log.d(TAG, "REQUEST_INSTALL_PACKAGES permission is granted.")

        if (currentDownloadId == null) {
            Log.e(TAG, "installApk: currentDownloadId is null. Cannot proceed with installation.")
            _uiState.value = _uiState.value.copy(error = "无法找到下载任务ID，无法安装。")
            return
        }
        Log.d(TAG, "installApk: Using currentDownloadId: $currentDownloadId to install APK.")

        try {
            // 获取下载文件的实际路径
            val latestVersion = _latestVersion.value
            if (latestVersion == null) {
                Log.e(TAG, "installApk: Latest version is null, cannot determine file name.")
                _uiState.value = _uiState.value.copy(error = "无法获取版本信息，无法安装。")
                return
            }
            
            val fileName = "VlogApp_${latestVersion.versionName ?: "latest"}.apk"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadDir, fileName)
            
            Log.d(TAG, "installApk: Looking for APK file at: ${apkFile.absolutePath}")
            
            if (!apkFile.exists()) {
                Log.e(TAG, "installApk: APK file does not exist at: ${apkFile.absolutePath}")
                _uiState.value = _uiState.value.copy(error = "下载的APK文件未找到，请重新下载。")
                return
            }
            
            // 使用FileProvider获取URI
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            Log.d(TAG, "installApk: APK URI from FileProvider: $apkUri")

            if (_uiState.value.requiresInstallPermission) { // Should be false if check above passed, but good for safety
                _uiState.value = _uiState.value.copy(requiresInstallPermission = false)
                Log.d(TAG, "installApk: Cleared requiresInstallPermission flag as install is proceeding.")
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            Log.d(TAG, "installApk: Install intent created: $installIntent")

            Log.i(TAG, "installApk: Launching install intent with URI: $apkUri for download ID: $currentDownloadId")
            context.startActivity(installIntent)
            Log.i(TAG, "installApk: Install activity started successfully for URI: $apkUri")

            _uiState.value = _uiState.value.copy(
                 isDownloading = false,
                 downloadCompleted = true
            )
            Log.d(TAG, "installApk: UI state updated: downloadCompleted=true after launching install intent.")
        } catch (e: Exception) {
            Log.e(TAG, "installApk: Install failed with exception", e)
            _uiState.value = _uiState.value.copy(
                error = "安装失败: ${e.message}"
            )
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        Log.d(TAG, "clearError called")
        _uiState.value = _uiState.value.copy(
            error = null,
            requiresStoragePermission = false,
            requiresInstallPermission = false
        )
        Log.d(TAG, "UI state error and permission flags cleared.")
    }

    /**
     * 重置下载状态
     */
    fun resetDownloadState() {
        Log.i(TAG, "resetDownloadState: User action to reset download state.")
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadCompleted = false,
            downloadId = null,
            error = null,
            requiresStoragePermission = false,
            requiresInstallPermission = false
        )
        Log.d(TAG, "resetDownloadState: UI download state reset.")
        Log.i(TAG, "resetDownloadState: Calling unregisterListeners.")
        unregisterListeners()
        currentDownloadId = null
        Log.d(TAG, "resetDownloadState: Current download ID cleared.")
    }
}

private fun AppUpdateViewModel.DownloadCompletionReceiver.unregister() { }

/**
 * 应用更新UI状态
 */
data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadCompleted: Boolean = false,
    val downloadId: Long? = null,
    val error: String? = null,
    val requiresStoragePermission: Boolean = false,
    val requiresInstallPermission: Boolean = false
)

/**
 * 当前应用版本信息
 */
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String
)