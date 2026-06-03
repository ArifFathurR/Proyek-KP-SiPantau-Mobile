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
        ProgresIndustriDigitalEntity::class,
        ProgresKeluargaEntity::class,
        ProgresPertanianEntity::class,
        DesaLocalEntity::class,
        LaporanPmlLocalEntity::class,
        PendingLaporanPmlEntity::class,
        SubslsLocalEntity::class
    ],
    version = 13,                 // ⚠️ Naikkan versi DB
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kegiatanDao(): KegiatanDao
    abstract fun pantauProgresDao(): PantauProgresDao
    abstract fun laporanDao(): LaporanDao
    abstract fun ProgresIndustriDigitalDao(): ProgresIndustriDigitalDao
    abstract fun progresKeluargaDao(): ProgresKeluargaDao
    abstract fun progresPertanianDao(): ProgresPertanianDao
    abstract fun laporanPmlDao(): LaporanPmlDao


    // ➕ DAO Baru
    abstract fun wilayahDao(): WilayahDao
    abstract fun subslsDao(): SubslsDao

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
