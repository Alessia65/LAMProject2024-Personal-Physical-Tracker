package com.example.personalphysicaltracker.activities

import android.util.Log
import com.example.personalphysicaltracker.database.LocationEntity
import com.example.personalphysicaltracker.viewModels.ActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationInfo {

    var start: String = ""
    var date: String = ""
    lateinit var end: String
    var latitude: Double = 0.0
    var longitude: Double = 0.0


    var duration: Double = 0.0
    protected lateinit var activityViewModel: ActivityViewModel

    fun initialize(lat: Double, long: Double, activityViewModel: ActivityViewModel){
        latitude = lat
        longitude = long
        start = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
        date = start.substring(0,10)
        this.activityViewModel = activityViewModel
        Log.d("LocationHandler", "Entrance time recorded: $start at $date")

    }


    fun setFinishTime(){
        end = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
        Log.d("LocationHandler", "Exit time recorded: $end")
        calculateDuration()
        CoroutineScope(Dispatchers.IO).launch {
            saveInDb()
        }
    }

    fun calculateDuration(){
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        this.duration =  (endTime - startTime).toDouble() / 1000
        Log.d("LocationHandler", "Duration recorded: $duration")

    }

     fun saveInDb(){
        val locationEntity = LocationEntity(
            latitude = this.latitude,
            longitude = this.longitude,
            date = this.date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertLocationInfo(locationEntity)
    }



}
