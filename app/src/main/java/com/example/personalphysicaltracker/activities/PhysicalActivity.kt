package com.example.personalphysicaltracker.activities

import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class PhysicalActivity : PhysicalActivityInterface {

    var date: String = (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).format(Date())
    var start: String = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
    lateinit var end: String
    var duration: Double = 0.0
    protected lateinit var activityDBViewModel: ActivityDBViewModel
    protected var activityType: ActivityType = ActivityType.UNKNOWN

    override fun setActivityViewModelVar(activityDBViewModel: ActivityDBViewModel){
        this.activityDBViewModel = activityDBViewModel
    }
    override fun getActivityTypeName(): ActivityType {
        return activityType
    }

    override fun setFinishTime(){
        end = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
    }

     override fun setFinishTimeWithString(finish: String) {
        end = finish
    }

    override fun setStartTimeWIthString(start: String) {
        this.start = start
    }

    override fun calculateDuration(): Double{
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        duration =  (endTime - startTime).toDouble() / 1000
        return duration
    }

    override suspend fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "UNKNOWN",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityDBViewModel.insertActivityEntity(activityEntity)

        Log.d("DATABASE", "saved UNKNOWN activity with start: $start, end: $end, duration: $duration")

    }



}
