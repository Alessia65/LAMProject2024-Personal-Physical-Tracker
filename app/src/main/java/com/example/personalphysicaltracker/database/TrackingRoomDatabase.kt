package com.example.personalphysicaltracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Database(entities = [ActivityEntity::class, WalkingActivityEntity::class, LocationEntity::class], version = 1, exportSchema = false)
abstract class TrackingRoomDatabase : RoomDatabase() {

    abstract fun trackingDao(): TrackingDao

    companion object {

        @Volatile
        private var INSTANCE: TrackingRoomDatabase? = null

        private const val nThreads: Int = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(nThreads)

        private val sRoomDatabaseCallback = object: Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                /* When the database gets called for the first time */
                databaseWriteExecutor.execute {
                    INSTANCE?.trackingDao()

                    /*
                    //val dao = INSTANCE?.trackingDao()
                    dao?.insert(ActivityEntity(activityType = "WALKING", date = "2024-06-15",timeStart = "06:00:00", timeFinish = "10:00:00", duration = 4 * 3600.0))

                   */

                }
            }
        }

fun getDatabase(context: Context) : TrackingRoomDatabase {
            return INSTANCE ?: synchronized (this) {
                val _INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    TrackingRoomDatabase::class.java,
                    "activity_database"
                ).addCallback(sRoomDatabaseCallback).build()
                INSTANCE = _INSTANCE
                _INSTANCE
            }
        }
    }
}