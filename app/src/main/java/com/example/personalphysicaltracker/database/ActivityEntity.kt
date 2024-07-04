package com.example.personalphysicaltracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities_table")
data class ActivityEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "activity_type")
    var activityType: String,

    var date: String,

    @ColumnInfo(name = "time_start")
    var timeStart: String,

    @ColumnInfo(name = "time_finish")
    var timeFinish: String,

    var duration: Double
)
