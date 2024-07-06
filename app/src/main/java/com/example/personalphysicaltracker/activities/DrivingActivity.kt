package com.example.personalphysicaltracker.activities

import android.util.Log
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
        activityDBViewModel.insertActivityEntity(activityEntity)

        Log.d("DRIVING ACTIVITY", "saved driving activity with start: $start, end: $end, duration: $duration")

    }

}
