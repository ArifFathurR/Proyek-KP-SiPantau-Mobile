// localData/AppDatabase.kt
package com.example.sipantau.localData

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sipantau.localData.dao.KegiatanDao
import com.example.sipantau.localData.entity.KegiatanEntity

@Database(entities = [KegiatanEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kegiatanDao(): KegiatanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sipantau_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
