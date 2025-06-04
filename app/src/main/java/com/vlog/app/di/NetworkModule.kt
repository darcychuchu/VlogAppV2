package com.vlog.app.di

import android.content.Context
import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vlog.app.data.database.VlogDatabase
import com.vlog.app.data.favorites.FavoriteService
import com.vlog.app.data.favorites.FavoritesDao
import com.vlog.app.data.histories.watch.WatchHistoryDao
import com.vlog.app.data.users.UserService
import com.vlog.app.data.videos.CategoryDao
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.VideoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton
import kotlin.apply

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    val APP_INFO = "${Build.BRAND}-${Build.PRODUCT}-VlogApp-${Constants.APP_VERSION}-0"

    val provideAuthInterceptor =  Interceptor { chain: Interceptor.Chain ->
        val initialRequest = chain.request()
        val newUrl = initialRequest.url.newBuilder()
            .addQueryParameter("app_info", APP_INFO)
            .build()
        val newRequest = initialRequest.newBuilder()
            .url(newUrl)
            .build()
        chain.proceed(newRequest)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply { eventListenerFactory(LoggingEventListener.Factory()) }
            .addInterceptor(provideAuthInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        val baseUrl = Constants.API_BASE_URL
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }



    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideVlogDatabase(@ApplicationContext context: Context): VlogDatabase {
        return VlogDatabase.getDatabase(context)
    }


    @Provides
    @Singleton
    fun provideVideoService(retrofit: Retrofit): VideoService {
        return retrofit.create(VideoService::class.java)
    }

    @Provides
    fun provideVideoDao(database: VlogDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    fun provideCategoryDao(database: VlogDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteService(retrofit: Retrofit): FavoriteService {
        return retrofit.create(FavoriteService::class.java)
    }

    @Provides
    fun provideFavoritesDao(database: VlogDatabase): FavoritesDao {
        return database.favoritesDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: VlogDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }



}
