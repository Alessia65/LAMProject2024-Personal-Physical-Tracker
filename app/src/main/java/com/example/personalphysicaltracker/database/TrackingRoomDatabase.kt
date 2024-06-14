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