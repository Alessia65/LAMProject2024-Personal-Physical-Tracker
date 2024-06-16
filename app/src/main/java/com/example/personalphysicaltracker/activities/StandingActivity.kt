package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*

class StandingActivity : PhysicalActivity() {


    override fun getActivityName(): String {
        return "Standing"
    }

    override fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "Standing",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }




}
