package com.example.personalphysicaltracker.activities


import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class PhysicalActivity : PhysicalActivityInterface {

    var date: String = (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).format(Date())
    var start: String = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
    lateinit var end: String
    var duration: Double = 0.0
    protected lateinit var activityViewModel: ActivityViewModel
    protected var activityType: ActivityType = ActivityType.UNKNOWN

    override fun setActivityViewModelVar(activityViewModel: ActivityViewModel){
        this.activityViewModel = activityViewModel
    }
    override fun getActivityTypeName(): ActivityType {
        return activityType
    }

    override fun setFinishTime(){
        end = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
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
        activityViewModel.insertActivityEntity(activityEntity)
    }



}
