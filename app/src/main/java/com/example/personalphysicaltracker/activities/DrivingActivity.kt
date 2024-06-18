package com.example.personalphysicaltracker.activities

import com.example.personalphysicaltracker.database.ActivityEntity

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
