package com.vlog.app.data.versions


data class AppVersion(
    var versionCode: Int = 0,          // 版本号 1
    var versionName: String? = null,       // 版本名称 APP_VERSION  = 1.0.0
    var forceUpdate: Boolean = false,      // 是否强制更新
    var downloadUrl: String? = null,       // APK下载地址
    var description: String? = null,       // 更新说明
    var fileSize: Long = 0,            // 文件大小（字节）
    var md5: String? = null               // APK文件MD5值，用于校验
)