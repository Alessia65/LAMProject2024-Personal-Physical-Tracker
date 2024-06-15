package com.example.personalphysicaltracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// exportSchema set to false to avoid DB migrations
@Database(entities = [ActivityEntity::class], version = 1, exportSchema = false)
abstract class TrackingRoomDatabase : RoomDatabase() {

    abstract fun trackingDao(): TrackingDao

    companion object {

        /* Per farne un singleton in maniera sicura.
            RoomDatabase non ha un costruttore, ogni volta che si vuole prendere il riferimento
            bisogna chiamare getDatabase (funzione statica perchè si trova in un companion object).
            Chiamandolo ritorna il db se non è null, se è null crea il database.
        */

        @Volatile
        private var INSTANCE: TrackingRoomDatabase? = null

        private const val nThreads: Int = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(nThreads)

        // TODO: sRoomDatabaseCallback: un modo per eseguire un frammento di codice al momento della creazione può servire o no
        private val sRoomDatabaseCallback = object: RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                /* What happens when the database gets called for the first time? */
                databaseWriteExecutor.execute() {
                    val dao = INSTANCE?.trackingDao()

                    //Impostare giorno 15/06 e orario 6.15pm
                    // Attività di tipo "Walking" con durate di 4 ore
                    dao?.insert(ActivityEntity(activityType = "WalkingActivity", date = "2024-06-15",timeStart = "06:00:00", timeFinish = "10:00:00", duration = 4 * 3600.0))

                    // Attività di tipo "Driving" con durate di 6
                    dao?.insert(ActivityEntity(activityType = "DrivingActivity", date = "2024-06-15",timeStart = "11:00:00", timeFinish = "17:00:00", duration = 6 * 3600.0))

                    // Attività di tipo "Standing" con durate di 1 ora
                    dao?.insert(ActivityEntity(activityType = "StandingActivity", date = "2024-06-15", timeStart = "17:10:00", timeFinish = "18:10:00", duration = 1 * 3600.0))

                }
            }
        }

        // INSTANCE = _INSTANCE: per via del synchronized serve una return di _INSTANCE in quante al momento della return INSTANCE è ancora null
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