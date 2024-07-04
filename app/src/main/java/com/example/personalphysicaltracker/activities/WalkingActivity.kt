package com.example.personalphysicaltracker.activities

import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import kotlinx.coroutines.*

class WalkingActivity : PhysicalActivity() {

    private var steps: Long = 0


    init{
        activityType = ActivityType.WALKING
    }

    fun getSteps(): Long{
        return steps
    }
    fun setActivitySteps(s: Long){
        steps = s
    }

    override suspend fun saveInDb() {
        val activityEntity = ActivityEntity(
            activityType = "WALKING",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )

        withContext(Dispatchers.IO) {
            activityDBViewModel.insertWalkingActivity(activityEntity, steps)
        }

        Log.d("DATABASE", "saved walking activity: $start, $end, $duration, $steps")
    }



}
