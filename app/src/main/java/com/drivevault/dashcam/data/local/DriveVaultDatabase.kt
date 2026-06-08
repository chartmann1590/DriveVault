package com.drivevault.dashcam.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.drivevault.dashcam.data.local.dao.*
import com.drivevault.dashcam.data.local.entity.*

@Database(
    entities = [
        ClipEntity::class,
        LocationSampleEntity::class,
        HeadingSampleEntity::class,
        SnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DriveVaultDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun locationSampleDao(): LocationSampleDao
    abstract fun headingSampleDao(): HeadingSampleDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: DriveVaultDatabase? = null

        fun getInstance(context: Context): DriveVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DriveVaultDatabase::class.java,
                    "drivevault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
