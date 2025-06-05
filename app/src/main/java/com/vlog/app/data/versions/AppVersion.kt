package com.vlog.app.data.versions


data class AppVersion(
    val versionCode: Int = 0,          // 版本号 1
    val versionName: String? = null,       // 版本名称 APP_VERSION  = 1.0.0
    val forceUpdate: Boolean = false,      // 是否强制更新
    val downloadUrl: String? = null,       // APK下载地址
    val description: String? = null,       // 更新说明
    val fileSize: Long = 0,            // 文件大小（字节）
    val md5: String? = null               // APK文件MD5值，用于校验
)