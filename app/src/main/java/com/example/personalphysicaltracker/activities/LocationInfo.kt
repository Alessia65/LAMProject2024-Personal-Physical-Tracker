package com.example.personalphysicaltracker.activities

import android.util.Log
import com.example.personalphysicaltracker.database.LocationEntity
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationInfo : LocationInfoInterface {

    var start: String = ""
    var date: String = ""
    var end: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var duration: Double = 0.0
    var saved = false

    private lateinit var activityDBViewModel: ActivityDBViewModel

    override fun initialize(lat: Double, long: Double, activityViewModel: ActivityDBViewModel){
        latitude = lat
        longitude = long
        start = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
        date = start.substring(0,10)
        this.activityDBViewModel = activityViewModel

    }



    override fun setFinishTime(){
        end = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
        calculateDuration()
        CoroutineScope(Dispatchers.IO).launch {
            saveInDb()
        }
    }

    override fun setFinishTimeWithString(finish: String) {
        end = finish
    }

    override fun setStartTimeWithString(start: String) {
        this.start = start
    }



    override fun calculateDuration(){
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        this.duration =  (endTime - startTime).toDouble() / 1000

    }

    override fun saveInDb(){

        if (!saved){
            val locationEntity = LocationEntity(
                latitude = this.latitude,
                longitude = this.longitude,
                date = this.date,
                timeStart = start,
                timeFinish = end,
                duration = duration
            )
            activityDBViewModel.insertLocationInfo(locationEntity)
            saved = true
            Log.d("LOCATION INFO", "Location saved in db with date: ${date}, start: ${start}, end: ${end}, duration: $duration")

        }


      }





}
