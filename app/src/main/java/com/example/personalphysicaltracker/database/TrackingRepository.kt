package com.example.mvvm_todolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.TrackingDao
import com.example.personalphysicaltracker.database.TrackingRoomDatabase


class TrackingRepository(app: Application) {

    var trackingDao: TrackingDao

    init {
        val db = TrackingRoomDatabase.getDatabase(app)
        trackingDao = db.trackingDao()
    }

    fun getAllActivities(): LiveData<List<ActivityEntity>> {

        return trackingDao.getListOfActivities()
    }

    fun insertActivityEntity(activityEntity: ActivityEntity) {
        TrackingRoomDatabase.databaseWriteExecutor.execute{
            trackingDao.insert(activityEntity)
        }
    }

    fun deleteListActivities(toDelete: List<ActivityEntity>) {
        TrackingRoomDatabase.databaseWriteExecutor.execute{
            trackingDao.deleteList(toDelete)
        }
    }
}