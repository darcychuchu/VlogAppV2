package com.vlog.app.di

import android.content.Context
import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vlog.app.data.cache.FilterUrlCacheDao
import com.vlog.app.data.categories.CategoryDao
import com.vlog.app.data.categories.CategoryService
import com.vlog.app.data.comments.CommentDao
import com.vlog.app.data.comments.CommentService
import com.vlog.app.data.database.VlogDatabase
import com.vlog.app.data.favorites.FavoriteService
import com.vlog.app.data.favorites.FavoritesDao
import com.vlog.app.data.histories.search.SearchHistoryDao
import com.vlog.app.data.histories.search.SearchRepository
import com.vlog.app.data.histories.search.SearchService
import com.vlog.app.data.histories.watch.WatchHistoryDao
import com.vlog.app.data.stories.StoriesService
import com.vlog.app.data.users.UserService
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.GatherItemDao
import com.vlog.app.data.videos.VideoService
import com.vlog.app.data.versions.AppUpdateService
import com.vlog.app.data.versions.AppUpdateRepository
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
    fun provideGatherItemDao(database: VlogDatabase): GatherItemDao {
        return database.gatherItemDao()
    }




    @Provides
    @Singleton
    fun provideCategoryService(retrofit: Retrofit): CategoryService {
        return retrofit.create(CategoryService::class.java)
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

//    @Provides
//    @Singleton
//    fun provideWatchHistoryRepository(
//        watchHistoryDao: WatchHistoryDao
//    ): WatchHistoryRepository {
//        return WatchHistoryRepository(watchHistoryDao)
//    }


    @Provides
    @Singleton
    fun provideSearchService(retrofit: Retrofit): SearchService {
        return retrofit.create(SearchService::class.java)
    }

    @Provides
    fun provideSearchHistoryDao(database: VlogDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        searchService: SearchService,
        searchHistoryDao: SearchHistoryDao
    ): SearchRepository {
        return SearchRepository(searchService, searchHistoryDao)
    }

    @Provides
    @Singleton
    fun provideAppUpdateService(retrofit: Retrofit): AppUpdateService {
        return retrofit.create(AppUpdateService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppUpdateRepository(
        appUpdateService: AppUpdateService
    ): AppUpdateRepository {
        return AppUpdateRepository(appUpdateService)
    }

    @Provides
    @Singleton
    fun provideStoriesService(retrofit: Retrofit): StoriesService {
        return retrofit.create(StoriesService::class.java)
    }

    @Provides
    @Singleton
    fun provideCommentService(retrofit: Retrofit): CommentService {
        return retrofit.create(CommentService::class.java)
    }

    @Provides
    fun provideCommentDao(database: VlogDatabase): CommentDao {
        return database.commentDao()
    }

    @Provides
    fun provideFilterUrlCacheDao(database: VlogDatabase): FilterUrlCacheDao {
        return database.filterUrlCacheDao()
    }


}
