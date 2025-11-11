package com.example.sipantau.localData

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sipantau.localData.dao.KegiatanDao
import com.example.sipantau.localData.dao.PantauProgresDao
import com.example.sipantau.localData.entity.KegiatanEntity
import com.example.sipantau.localData.entity.PantauProgresEntity

@Database(
    entities = [KegiatanEntity::class, PantauProgresEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kegiatanDao(): KegiatanDao
    abstract fun pantauProgresDao(): PantauProgresDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sipantau_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
