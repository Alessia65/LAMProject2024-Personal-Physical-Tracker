package com.example.personalphysicaltracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations_table")
data class LocationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    var latitude: Double,

    var longitude: Double,

    val date: String,

    @ColumnInfo(name = "date_time_start")
    var timeStart: String,

    @ColumnInfo(name = "date_time_finish")
    var timeFinish: String,

    var duration: Double
)
