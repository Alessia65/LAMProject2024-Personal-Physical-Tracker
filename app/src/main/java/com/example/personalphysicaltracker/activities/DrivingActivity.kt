package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*

class DrivingActivity: PhysicalActivity() {


    override fun getActivityName(): String {
        return "Driving"
    }

    override fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "Driving",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }

}
