package com.vlog.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.vlog.app.data.cache.FilterUrlCacheDao
import com.vlog.app.data.cache.FilterUrlCacheEntity
import com.vlog.app.data.categories.CategoryDao
import com.vlog.app.data.categories.CategoriesEntity
import com.vlog.app.data.comments.CommentDao
import com.vlog.app.data.comments.CommentEntity
import com.vlog.app.data.favorites.FavoritesDao
import com.vlog.app.data.favorites.FavoritesEntity
import com.vlog.app.data.histories.search.SearchHistoryDao
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.histories.watch.WatchHistoryDao
import com.vlog.app.data.histories.watch.WatchHistoryEntity
import com.vlog.app.data.videos.GatherItemDao
import com.vlog.app.data.videos.GatherItemEntity
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        CategoriesEntity::class,
        FavoritesEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class,
        GatherItemEntity::class,
        CommentEntity::class,
        FilterUrlCacheEntity::class // Added FilterUrlCacheEntity
    ],
    version = 3, // Incremented version for schema change
    exportSchema = false
)
abstract class VlogDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun gatherItemDao(): GatherItemDao
    abstract fun commentDao(): CommentDao
    abstract fun filterUrlCacheDao(): FilterUrlCacheDao // Added FilterUrlCacheDao accessor

    companion object {
        @Volatile
        private var INSTANCE: VlogDatabase? = null

//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS `favorite_videos` (
//                        `videoId` TEXT NOT NULL,
//                        `title` TEXT,
//                        `coverUrl` TEXT,
//                        `score` TEXT,
//                        `tags` TEXT,
//                        `remarks` TEXT,
//                        `publishedAt` TEXT,
//                        `categoryId` TEXT,
//                        `isTyped` INTEGER,
//                        `version` INTEGER,
//                        `createdAt` INTEGER NOT NULL,
//                        PRIMARY KEY(`videoId`)
//                    )
//                """)
//            }
//        }

        fun getDatabase(context: Context): VlogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VlogDatabase::class.java,
                    "vlog_database"
                )
                    //.addMigrations(MIGRATION_1_2) // Example: Add specific migrations if needed
                    .fallbackToDestructiveMigration() // Added for schema change handling
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}