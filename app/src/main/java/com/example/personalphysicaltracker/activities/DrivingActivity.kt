package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*

class DrivingActivity: PhysicalActivity() {

    init{
        activityType = ActivityType.DRIVING
    }

    override suspend fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "DRIVING",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }

}
