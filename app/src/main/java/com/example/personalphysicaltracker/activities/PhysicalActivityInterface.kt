package com.example.personalphysicaltracker.activities

import com.example.personalphysicaltracker.viewModels.ActivityViewModel

interface PhysicalActivityInterface {
    fun getActivityTypeName(): ActivityType
    fun setActivityViewModelVar(activityViewModel: ActivityViewModel)

    fun setFinishTime()

    fun calculateDuration(): Double

    fun setFinishTimeWithString(finish: String)

    fun setStartTimeWIthString(start: String)
    suspend fun saveInDb()


}
