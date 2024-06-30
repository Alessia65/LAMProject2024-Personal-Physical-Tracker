package com.example.personalphysicaltracker.database

import android.app.Application
import android.util.Log
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

    suspend fun insertWalkingActivity(activityEntity: ActivityEntity, steps: Long) {
        // Insert the activity entity
        val activityId = trackingDao.insert(activityEntity)
        Log.d("TrackingRepository", "Inserted activity with ID: $activityId")

        // Ensure the activity entity is inserted before retrieving the ID
        if (activityId <= 0) {
            Log.e("TrackingRepository", "Failed to insert activity entity")
            throw Exception("Failed to insert activity entity")
        }

        // Insert the walking activity entity
        val walkingActivity = WalkingActivityEntity(walkingId = activityId.toInt(), steps = steps)
        trackingDao.insertWalkingActivityEntity(walkingActivity)
        Log.d("TrackingRepository", "Successfully inserted walking activity with ID: $activityId")
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

     fun insertLocationInfo(locationEntity: LocationEntity) {
        return trackingDao.insertLocation(locationEntity)
    }

    suspend fun getTotalPresenceInLocation(latitude: Double, longitude: Double, startDate:String, endDate:String): Double {
        return trackingDao.getTotalPresenceInLocation(latitude,longitude, startDate, endDate)
    }
}