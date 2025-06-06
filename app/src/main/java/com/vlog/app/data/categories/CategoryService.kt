package com.vlog.app.data.categories

import com.vlog.app.data.ApiResponse
import com.vlog.app.di.Constants
import retrofit2.http.GET
import retrofit2.http.Query

interface CategoryService {

    @GET(Constants.ENDPOINT_VIDEO_CATEGORIES)
    suspend fun getCategoryList(
        @Query("token") token: String? = null
    ): ApiResponse<List<Categories>>
}