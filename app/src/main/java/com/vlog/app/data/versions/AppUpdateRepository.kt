package com.vlog.app.data.versions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用更新仓库
 * 负责从API获取应用版本信息
 */
class AppUpdateRepository(
    private val appUpdateService: AppUpdateService
) {
    /**
     * 检查应用更新
     * @return 应用版本信息
     */
    suspend fun checkUpdate(): Result<AppVersion> = withContext(Dispatchers.IO) {
        try {
            try {
                // 尝试调用真实API
                val response = appUpdateService.checkUpdate()
                if (response.code == 200) {
                    Result.success(response.data)
                }
                Result.success(AppVersion())
            } catch (e: Exception) {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}