package com.vlog.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.vlog.app.data.favorites.FavoriteVideoDao
import com.vlog.app.data.favorites.FavoriteVideoEntity
import com.vlog.app.data.histories.watch.WatchHistoryDao
import com.vlog.app.data.histories.watch.WatchHistoryEntity
import com.vlog.app.data.videos.*

@Database(
    entities = [
        VideoEntity::class,
        CategoryEntity::class,
        FavoriteVideoEntity::class,
        WatchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VlogDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteVideoDao(): FavoriteVideoDao
    abstract fun watchHistoryDao(): WatchHistoryDao

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