package com.vlog.app.ui.screens.publish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.ApiResponseCode
import com.vlog.app.data.stories.StoriesRepository
import com.vlog.app.data.users.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * 图文发布 ViewModel
 */
@HiltViewModel
class PhotoPublishViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val userSessionManager: UserSessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 表单状态
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags

    // 照片列表
    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 发布成功状态
    private val _publishSuccess = MutableStateFlow(false)
    val publishSuccess: StateFlow<Boolean> = _publishSuccess

    // 更新标题
    fun updateTitle(title: String) {
        _title.value = title
    }

    // 更新描述
    fun updateDescription(description: String) {
        _description.value = description
    }

    // 更新标签
    fun updateTags(tags: String) {
        _tags.value = tags
    }

    // 添加照片
    fun addPhoto(photoUri: Uri) {
        _photos.value = _photos.value + photoUri
    }

    // 移除照片
    fun removePhoto(photoUri: Uri) {
        _photos.value = _photos.value.filter { it != photoUri }
    }

    // 清空照片
    fun clearPhotos() {
        _photos.value = emptyList()
    }

    // 验证表单
    fun validateForm(): Boolean {
        // 描述是必填项
        if (_description.value.isBlank()) {
            _error.value = "描述不能为空"
            return false
        }

        // 至少需要一张照片
        if (_photos.value.isEmpty()) {
            _error.value = "请至少添加一张照片"
            return false
        }

        return true
    }

    // 发布图文
    fun publishStory() {
        if (!validateForm()) {
            return
        }

        val userName = userSessionManager.getUserName()
        val token = userSessionManager.getAccessToken()

        if (userName.isNullOrBlank() || token.isNullOrBlank()) {
            _error.value = "用户未登录"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // 准备参数
                val title = _title.value.takeIf { it.isNotBlank() }
                val description = _description.value
                val tags = _tags.value.takeIf { it.isNotBlank() }

                // 准备照片文件
                val photoFiles = _photos.value.mapNotNull { uri ->
                    try {
                        // 获取文件路径
                        val file = getFileFromUri(uri)
                        if (file != null) {
                            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("photoFile", file.name, requestBody)
                        } else {
                            Log.e("PhotoPublishViewModel", "无法获取文件: $uri")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("PhotoPublishViewModel", "处理照片文件失败: ${e.message}")
                        null
                    }
                }

                if (photoFiles.isEmpty()) {
                    _error.value = "照片处理失败"
                    _isLoading.value = false
                    return@launch
                }

                // 发布图文
                val response = storiesRepository.createStories(
                    name = userName,
                    token = token,
                    photoFiles = photoFiles,
                    title = title,
                    description = description,
                    tags = tags
                )

                if (response.code == ApiResponseCode.SUCCESS) {
                    // 发布成功
                    _publishSuccess.value = true
                    // 清空表单
                    _title.value = ""
                    _description.value = ""
                    _tags.value = ""
                    _photos.value = emptyList()
                } else {
                    // 发布失败
                    _error.value = response.message ?: "发布失败"
                }
            } catch (e: Exception) {
                _error.value = "发布失败: ${e.message}"
                Log.e("PhotoPublishViewModel", "发布失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 从 Uri 获取文件并压缩图片
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val file = File(uri.path ?: return null)
                    compressImage(file)
                }
                "content" -> {
                    // 从内容 Uri 复制到临时文件
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    compressImage(tempFile)
                }
                else -> null
            }
        } catch (e: IOException) {
            Log.e("PhotoPublishViewModel", "获取文件失败: ${e.message}")
            null
        }
    }

    // 压缩图片
    private fun compressImage(file: File): File {
        try {
            // 读取图片
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.path, options)

            // 计算压缩比例
            val maxSize = 1024 // 最大尺寸为 1024px
            var scale = 1
            if (options.outWidth > maxSize || options.outHeight > maxSize) {
                scale = (options.outWidth / maxSize).coerceAtLeast(options.outHeight / maxSize)
            }

            // 重新读取图片并压缩
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = scale
            }

            val bitmap = BitmapFactory.decodeFile(file.path, options)

            // 如果图片太大，进一步压缩质量
            val compressedFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            compressedFile.outputStream().use { output ->
                // 压缩质量为 80%
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }

            // 释放原始 Bitmap 内存
            bitmap.recycle()

            Log.d("PhotoPublishViewModel", "原始文件大小: ${file.length() / 1024}KB, 压缩后: ${compressedFile.length() / 1024}KB")

            return compressedFile
        } catch (e: Exception) {
            Log.e("PhotoPublishViewModel", "压缩图片失败: ${e.message}")
            return file // 如果压缩失败，返回原始文件
        }
    }

    // 清除错误
    fun clearError() {
        _error.value = null
    }

    // 重置发布成功状态
    fun resetPublishSuccess() {
        _publishSuccess.value = false
    }
}
