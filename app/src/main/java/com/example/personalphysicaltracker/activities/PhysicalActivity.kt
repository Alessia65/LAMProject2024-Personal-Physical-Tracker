package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.TrackingRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*

open class PhysicalActivity : PhysicalActivityInterface {

    public var date: String = (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).format(Date())
    public var start: String = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
    public lateinit var end: String
    public var duration: Double = 0.0
    protected lateinit var activityViewModel: ActivityViewModel
    protected var activityType: ActivityType

    init{
        activityType = ActivityType.UNKNOWN
    }
    override fun setActivityViewModelVar(actViewModel: ActivityViewModel){
        this.activityViewModel = actViewModel
    }
    override fun getActivityTypeName(): ActivityType {
        return activityType
    }

    override fun setFinishTime(){
        end = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())
    }

    override fun calculateDuration(): Double{
        if (start != null && end != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTime = dateFormat.parse(start)?.time ?: 0L
            val endTime = dateFormat.parse(end)?.time ?: 0L
            duration =  (endTime - startTime).toDouble() / 1000
            return duration
        }
        return 0.0
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
