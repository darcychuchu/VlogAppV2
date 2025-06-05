package com.vlog.app.data.versions

import com.vlog.app.data.videos.VideoList
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
    suspend fun checkUpdate(): AppVersion? {
        return withContext(Dispatchers.IO) {
            appUpdateService.checkUpdate().data
        }
    }
}