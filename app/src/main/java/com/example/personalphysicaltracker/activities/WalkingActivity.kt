package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WalkingActivity : PhysicalActivity() {

    private var steps: Long = 0


    override fun getActivityName(): String {
        return "Walking"
    }

    fun setActivitySteps(s: Long){
        steps = s
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
