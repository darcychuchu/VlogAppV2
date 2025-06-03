package com.vlog.app.data

/**
 * API 响应码常量
 * 用于统一管理 API 响应码，避免硬编码
 */
object ApiResponseCode {
    // 成功响应码
    const val SUCCESS = 200

    // 错误响应码基线
    const val ERROR_BASELINE = 400

    // 客户端错误码 (4xx)
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404

    // 服务器错误码 (5xx)
    const val SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503

    // 自定义业务错误码
    const val VALIDATION_ERROR = 422
    const val BUSINESS_ERROR = 460
}