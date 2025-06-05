package com.vlog.app.data.versions

import com.vlog.app.data.ApiResponse
import com.vlog.app.di.Constants
import retrofit2.http.*

interface AppUpdateService {
    // 应用更新接口
    @GET(Constants.ENDPOINT_CHECK_UPDATE)
    suspend fun checkUpdate(): ApiResponse<AppVersion>
}
