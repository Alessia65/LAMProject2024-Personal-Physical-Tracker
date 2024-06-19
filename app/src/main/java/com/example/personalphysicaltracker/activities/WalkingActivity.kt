package com.example.personalphysicaltracker.activities

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

        // to complete the insert before taking id
        withContext(Dispatchers.IO) {
            activityViewModel.insertActivityEntity(activityEntity)

        }

        val id = activityViewModel.getLastWalkingId()

        activityViewModel.insertWalkingActivityEntity(id, steps)
    }


}
