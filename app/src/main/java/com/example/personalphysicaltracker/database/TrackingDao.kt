package com.example.personalphysicaltracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface TrackingDao {

    @Query("SELECT * FROM steps_table WHERE walkingActivityId = :id")
    suspend fun getWalkingActivityById(id: Int): WalkingActivityEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWalkingActivityEntity(walkingActivity: WalkingActivityEntity)

    @Query("SELECT SUM(steps) FROM steps_table JOIN activities_table ON steps_table.walkingActivityId = activities_table.id WHERE activities_table.date = :date AND activities_table.activity_type = 'WALKING'")
    suspend fun getTotalStepsFromToday(date:String): Long

    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType AND date = :day")
    suspend fun getTotalDurationByActivityTypeInDay(day:String, activityType:String): Double

    @Query("SELECT * FROM activities_table ORDER BY time_finish DESC LIMIT 1")
    suspend fun getLastActivity(): ActivityEntity?

    @Query("SELECT * FROM activities_table")
    suspend fun getListOfActivities(): List<ActivityEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activity: ActivityEntity): Long


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocation(location: LocationEntity)

    @Query("SELECT SUM(duration) FROM locations_table WHERE (latitude = :latitude AND longitude = :longitude AND date >= :startDate AND date <= :endDate)")
    suspend fun getTotalPresenceInLocation(latitude: Double, longitude: Double, startDate: String, endDate: String): Double

    @Query("SELECT * FROM locations_table WHERE (date >= :startDate AND date <= :endDate)")
    suspend fun getAllLocationsInDate(startDate: String, endDate: String): List<LocationEntity>

}