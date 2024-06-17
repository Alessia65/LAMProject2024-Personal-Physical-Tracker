package com.example.personalphysicaltracker.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.*

//Chiamate che si possono fare al database

@Dao
interface TrackingDao {

    //TODO: Fare le query che servono

    //Insert per seconda tabella
    @Query("INSERT INTO steps_table VALUES(:id, :steps)")
    suspend fun insertWalkingActivityEntity(id: Int, steps: Long)

    @Query("SELECT SUM(steps) FROM steps_table JOIN activities_table ON steps_table.walkingActivityId = activities_table.id WHERE activities_table.date = :date AND activities_table.activity_type = 'Walking'")
    suspend fun getTotalStepsFromToday(date:String): Long

    @Query("SELECT id FROM activities_table WHERE activity_type='Walking' ORDER BY id DESC LIMIT 1")
    suspend fun getLastWalkingId(): Int
    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType AND date = :day")
    suspend fun getTotalDurationByActivityTypeInDay(day:String, activityType:String): Double

    @Query("SELECT * FROM activities_table ORDER BY time_finish DESC LIMIT 1")
    suspend fun getLastActivity(): ActivityEntity?

    @Query("SELECT * FROM activities_table")
    fun getListOfActivities(): LiveData<List<ActivityEntity>>

    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType")
    fun getTotalDurationByActivityType(activityType: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultiple(activities: List<ActivityEntity>)


    @Delete
    fun deleteList(activitiesList: List<ActivityEntity>)

    //@Query("DELETE FROM todo_table WHERE id in (:idList)")
    //fun deleteDone(idList: List<String>)
}