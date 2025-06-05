package com.vlog.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.vlog.app.data.favorites.FavoritesDao
import com.vlog.app.data.favorites.FavoritesEntity
import com.vlog.app.data.histories.search.SearchHistoryDao
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.histories.watch.WatchHistoryDao
import com.vlog.app.data.histories.watch.WatchHistoryEntity
import com.vlog.app.data.videos.*

@Database(
    entities = [
        VideoEntity::class,
        CategoryEntity::class,
        FavoritesEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VlogDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

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
                    //.addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}