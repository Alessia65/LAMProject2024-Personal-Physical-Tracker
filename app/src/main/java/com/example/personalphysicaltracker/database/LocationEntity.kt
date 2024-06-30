package com.example.personalphysicaltracker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations_table")
data class LocationEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "latitude")
    var latitude: Double,

    @ColumnInfo(name = "longitude")
    var longitude: Double,

    @ColumnInfo(name = "date_time_start")
    var timeStart: String,

    @ColumnInfo(name = "date_time_finish")
    var timeFinish: String,

    @ColumnInfo(name = "duration")
    var duration: Double
)
