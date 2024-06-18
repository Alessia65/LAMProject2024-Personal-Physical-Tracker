package com.example.personalphysicaltracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// exportSchema set to false to avoid DB migrations
@Database(entities = [ActivityEntity::class, WalkingActivityEntity::class], version = 1, exportSchema = false)
abstract class TrackingRoomDatabase : RoomDatabase() {

    abstract fun trackingDao(): TrackingDao

    companion object {

        /*
        To make it a singleton safely.
            RoomDatabase does not have a constructor, whenever you want to take the reference
            you need to call getDatabase (static function because it is found in a companion object).
            Calling it returns the db if it is not null, if it is null it creates the database.
        */

        @Volatile
        private var INSTANCE: TrackingRoomDatabase? = null

        private const val nThreads: Int = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(nThreads)

        //sRoomDatabaseCallback: a way to execute a code fragment at creation time may or may not help
        private val sRoomDatabaseCallback = object: Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                /* What happens when the database gets called for the first time? */
                databaseWriteExecutor.execute {
                    INSTANCE?.trackingDao()

                    /*
                    
                    //val dao = INSTANCE?.trackingDao()
                    //Day 15/06 and time 6.15pm
                    dao?.insert(ActivityEntity(activityType = "WALKING", date = "2024-06-15",timeStart = "06:00:00", timeFinish = "10:00:00", duration = 4 * 3600.0))

                    dao?.insert(ActivityEntity(activityType = "DRIVING", date = "2024-06-15",timeStart = "11:00:00", timeFinish = "17:00:00", duration = 6 * 3600.0))

                    dao?.insert(ActivityEntity(activityType = "STANDING", date = "2024-06-15", timeStart = "17:10:00", timeFinish = "18:10:00", duration = 1 * 3600.0))
                    */

                }
            }
        }

// INSTANCE = _INSTANCE: due to synchronized, a return of _INSTANCE is needed since INSTANCE is still null at the time of the return
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