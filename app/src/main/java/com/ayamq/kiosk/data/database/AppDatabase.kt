package com.ayamq.kiosk.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ayamq.kiosk.data.database.dao.MenuDao
import com.ayamq.kiosk.data.database.dao.OrderDao
import com.ayamq.kiosk.data.database.dao.OrderItemDao
import com.ayamq.kiosk.data.database.entity.MenuEntity
import com.ayamq.kiosk.data.database.entity.OrderEntity
import com.ayamq.kiosk.data.database.entity.OrderItemEntity

/**
 * Room Database for AyamQ Kiosk
 * Manages all database tables and provides DAOs for data access
 *
 * This is a singleton class - only one instance exists throughout the app lifecycle
 */
@Database(
    entities = [
        MenuEntity::class,
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 3, // Incremented version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract methods to get DAOs
    abstract fun menuDao(): MenuDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance using Double-Check Locking pattern
         * Thread-safe singleton implementation
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ayamq_database"
                )
                    // Recreate database on schema change (for development)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}