package com.vlog.app.data.database

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 基础仓库接口
 * 提供统一的错误处理和数据加载机制
 */
interface BaseRepository {
    companion object {
        private const val TAG = "BaseRepository"
    }
    /**
     * 安全地执行网络请求
     * @param apiCall 网络请求函数
     * @return 请求结果，包装在Result中
     */
    suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(apiCall.invoke())
            } catch (throwable: Throwable) {
                Log.e(TAG, "Safe API call error: ${throwable.message}", throwable)
                when (throwable) {
                    is SocketTimeoutException -> Result.failure(ApiError.Timeout(throwable))
                    is UnknownHostException -> Result.failure(ApiError.NoInternet(throwable))
                    is IOException -> Result.failure(ApiError.NetworkError(throwable))
                    is HttpException -> {
                        val code = throwable.code()
                        val errorBody = throwable.response()?.errorBody()?.string()
                        Result.failure(ApiError.ServerError(code, errorBody, throwable))
                    }

                    else -> Result.failure(ApiError.UnknownError(throwable))
                }
            }
        }
    }


    /**
     * 从本地数据源获取数据，如果本地数据为空或过期，则从网络获取
     * @param localDataSource 获取本地数据的函数
     * @param remoteDataSource 获取网络数据的函数
     * @param saveRemoteData 保存网络数据到本地的函数
     * @param shouldFetch 是否应该从网络获取数据的条件函数
     */
    fun <T> getDataWithCache(
        localDataSource: suspend () -> T,
        remoteDataSource: suspend () -> T,
        saveRemoteData: suspend (T) -> Unit,
        shouldFetch: suspend (T) -> Boolean = { true }
    ): Flow<Resource<T>> = flow {
        // 发送加载状态
        emit(Resource.Loading())

        try {
            // 尝试从本地获取数据
            val localData = localDataSource()

            // 如果本地数据不为空，先发送本地数据
            if (localData != null) {
                emit(Resource.Success(localData))
            }

            // 判断是否需要从网络获取数据
            val shouldFetchFromNetwork = shouldFetch(localData)

            if (shouldFetchFromNetwork) {
                try {
                    // 从网络获取数据
                    val remoteData = remoteDataSource()

                    // 保存到本地
                    saveRemoteData(remoteData)

                    // 发送网络数据
                    emit(Resource.Success(remoteData))
                } catch (e: Exception) {
                    // 如果网络请求失败，但本地有数据，则发送本地数据
                    if (localData != null) {
                        emit(Resource.Error("无法从网络获取数据，使用本地缓存", localData))
                    } else {
                        emit(Resource.Error("无法获取数据: ${e.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            // 如果本地数据获取失败，尝试从网络获取
            try {
                val remoteData = remoteDataSource()
                saveRemoteData(remoteData)
                emit(Resource.Success(remoteData))
            } catch (e2: Exception) {
                emit(Resource.Error("无法获取数据: ${e2.message}"))
            }
        }
    }.flowOn(Dispatchers.IO)
}

/**
 * API错误类型
 */
sealed class ApiError(message: String, cause: Throwable) : Exception(message, cause) {
    class Timeout(cause: Throwable) : ApiError("请求超时", cause)
    class NoInternet(cause: Throwable) : ApiError("无网络连接", cause)
    class NetworkError(cause: Throwable) : ApiError("网络错误", cause)
    class ServerError(val code: Int, val body: String?, cause: Throwable) :
        ApiError("服务器错误: $code", cause)
    class UnknownError(cause: Throwable) : ApiError("未知错误", cause)
}

/**
 * 资源包装类
 * 用于表示数据加载状态
 */
sealed class Resource<out T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
