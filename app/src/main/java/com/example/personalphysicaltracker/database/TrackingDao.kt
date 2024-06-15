package com.example.personalphysicaltracker.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//Chiamate che si possono fare al database

@Dao
interface TrackingDao {

    //TODO: Fare le query che servono

    @Query("SELECT * FROM activities_table ORDER BY date_finish DESC LIMIT 1")
    suspend fun getLastActivity(): ActivityEntity?

    @Query("SELECT * FROM activities_table")
    fun getListOfActivities(): LiveData<List<ActivityEntity>>

    @Query("SELECT SUM(duration) FROM activities_table WHERE activity_type = :activityType")
    fun getTotalDurationByActivityType(activityType: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMultiple(activities: List<ActivityEntity>)

    @Delete
    fun deleteList(activitiesList: List<ActivityEntity>)

    //@Query("DELETE FROM todo_table WHERE id in (:idList)")
    //fun deleteDone(idList: List<String>)
}