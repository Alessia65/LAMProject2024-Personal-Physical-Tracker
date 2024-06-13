package com.example.personalphysicaltracker.activities

interface ActivityInterface {
    fun getActivityName(): String
    fun startSensor()
    fun stopActivity()
}
