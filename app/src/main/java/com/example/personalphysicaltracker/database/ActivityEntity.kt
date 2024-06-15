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

    @ColumnInfo(name = "date_start")
    var dateStart: String,

    @ColumnInfo(name = "date_finish")
    var dateFinish: String,

    @ColumnInfo(name = "duration")
    var duration: Double
)
