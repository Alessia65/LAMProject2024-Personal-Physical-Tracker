package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class DrivingActivity(context: Context) : PhysicalActivity(context) {

    private var sensorJob: Job? = null
    private var startTimeMillis: Long = 0

    override fun getActivityName(): String {
        return "Driving Activity"
    }

}
