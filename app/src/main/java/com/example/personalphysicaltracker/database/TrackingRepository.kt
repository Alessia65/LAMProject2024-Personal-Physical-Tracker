package com.example.personalphysicaltracker.database

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity


class TrackingRepository(app: Application) {

    private var trackingDao: TrackingDao

    init {
        val db = TrackingRoomDatabase.getDatabase(app)
        trackingDao = db.trackingDao()
    }



    fun insertActivityEntity(activityEntity: ActivityEntity) {
        TrackingRoomDatabase.databaseWriteExecutor.execute{
            trackingDao.insert(activityEntity)
        }
    }


    suspend fun getLastActivity(): ActivityEntity? {
        return trackingDao.getLastActivity()
    }

    suspend fun getTotalDurationByActivityTypeInDay(day: String, activityType: String): Double{
        return trackingDao.getTotalDurationByActivityTypeInDay(day, activityType)
    }

    suspend fun getLastWalkingId(): Int{
        return trackingDao.getLastWalkingId()
    }

    suspend fun insertWalkingActivityEntity(id: Int, steps: Long){
        return trackingDao.insertWalkingActivityEntity(id, steps)
    }

    suspend fun getTotalStepsFromToday(date:String): Long{
        return  trackingDao.getTotalStepsFromToday(date)
    }

    suspend fun getListOfActivities(): List<ActivityEntity> {
        return trackingDao.getListOfActivities()
    }

    suspend fun getWalkingActivityById(id: Int): WalkingActivityEntity {
        return trackingDao.getWalkingActivityById(id)
    }
}