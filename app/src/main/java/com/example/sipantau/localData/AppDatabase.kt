package com.example.sipantau.localData

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sipantau.localData.dao.*
import com.example.sipantau.localData.entity.*

@Database(
    entities = [
        KegiatanEntity::class,
        PantauProgresEntity::class,
        PendingLaporanEntity::class,
        LaporanLocalEntity::class,
        KecamatanLocalEntity::class,
        DesaLocalEntity::class
    ],
    version = 4,                 // ⚠️ Naikkan versi DB
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kegiatanDao(): KegiatanDao
    abstract fun pantauProgresDao(): PantauProgresDao
    abstract fun laporanDao(): LaporanDao

    // ➕ DAO Baru
    abstract fun wilayahDao(): WilayahDao

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
