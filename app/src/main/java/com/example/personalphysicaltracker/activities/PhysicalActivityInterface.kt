package com.example.personalphysicaltracker.activities

interface PhysicalActivityInterface {
    fun getActivityName(): String
    fun startAccelerometer()
    fun stopActivity()
}
