package com.vlog.app.data

data class ApiResponse<T>(
    val code: Int = ApiResponseCode.SUCCESS,  // 默认值设为200
    val message: String? = null,
    val data: T? = null
)

data class PaginatedResponse<T>(
    val items: List<T>?,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10
)