package com.example.personalphysicaltracker.activities

import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel

interface LocationInfoInterface {

    fun initialize(lat: Double, long: Double, activityViewModel: ActivityDBViewModel)

    fun setFinishTime()

    fun setFinishTimeWithString(finish: String)

    fun setStartTimeWithString(start: String)

    fun calculateDuration()

    fun saveInDb()

}
