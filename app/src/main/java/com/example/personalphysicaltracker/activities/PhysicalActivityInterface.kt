package com.example.personalphysicaltracker.activities

import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel

interface PhysicalActivityInterface {
    fun getActivityTypeName(): ActivityType
    fun setActivityViewModelVar(activityDBViewModel: ActivityDBViewModel)

    fun setFinishTime()

    fun calculateDuration(): Double

    fun setFinishTimeWithString(finish: String)

    fun setStartTimeWIthString(start: String)
    suspend fun saveInDb()


}
