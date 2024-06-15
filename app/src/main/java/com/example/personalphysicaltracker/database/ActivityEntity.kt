package com.example.personalphysicaltracker.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities_table")
data class ActivityEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "activity_type")
    var activityType: String,

    @ColumnInfo(name = "date")
    var date: String,

    @ColumnInfo(name = "time_start")
    var timeStart: String,

    @ColumnInfo(name = "time_finish")
    var timeFinish: String,

    @ColumnInfo(name = "duration")
    var duration: Double
)
