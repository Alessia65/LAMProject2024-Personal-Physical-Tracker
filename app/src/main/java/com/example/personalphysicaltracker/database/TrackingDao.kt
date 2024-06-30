package com.example.personalphysicaltracker.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personalphysicaltracker.activities.WalkingActivity


@Dao
interface TrackingDao {

    @Query("SELECT * FROM steps_table WHERE walkingActivityId = :id")
    suspend fun getWalkingActivityById(id: Int): WalkingActivityEntity

    //Insert for step_table
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalkingActivityEntity(walkingActivity: WalkingActivityEntity)

    @Query("SELECT SUM(steps) FROM steps_table JOIN activities_table ON steps_table.walkingActivityId = activities_table.id WHERE activities_table.date = :date AND activities_table.activity_type = 'WALKING'")
    suspend fun getTotalStepsFromToday(date:String): Long

    @Query("SELECT id FROM activities_table WHERE activity_type='WALKING' ORDER BY id DESC LIMIT 1")
    suspend fun getLastWalkingId(): Int
    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType AND date = :day")
    suspend fun getTotalDurationByActivityTypeInDay(day:String, activityType:String): Double

    @Query("SELECT * FROM activities_table ORDER BY time_finish DESC LIMIT 1")
    suspend fun getLastActivity(): ActivityEntity?

    @Query("SELECT * FROM activities_table")
    fun getListOfActivities(): List<ActivityEntity>

    @Query("SELECT * FROM steps_table")
    fun getListOfWalkingActivities(): List<WalkingActivityEntity>

    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType")
    fun getTotalDurationByActivityType(activityType: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultiple(activities: List<ActivityEntity>)


    @Delete
    fun deleteList(activitiesList: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocation(location: LocationEntity)

    @Delete
    fun deleteLocation(location: LocationEntity)
}