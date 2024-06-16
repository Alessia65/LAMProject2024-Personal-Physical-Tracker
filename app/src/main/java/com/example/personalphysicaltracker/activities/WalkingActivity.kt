package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WalkingActivity : PhysicalActivity() {



    override fun getActivityName(): String {
        return "Walking"
    }

    override fun saveInDb(){
        val activityEntity = ActivityEntity(
            activityType = "Walking",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
        //TODO: aggiungere step
    }


}
