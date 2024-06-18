package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*

class StandingActivity : PhysicalActivity() {


    init{
        activityType = ActivityType.STANDING
    }
    override suspend fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "STANDING",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }




}
