package com.example.personalphysicaltracker.activities


import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity

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
        activityDBViewModel.insertActivityEntity(activityEntity)

        Log.d("DATABASE", "saved standing activity with start: $start, end: $end, duration: $duration")

    }




}
