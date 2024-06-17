package com.example.personalphysicaltracker.sensors

interface StepCounterListener {
    fun onStepCounterDataReceived(data: String)
}