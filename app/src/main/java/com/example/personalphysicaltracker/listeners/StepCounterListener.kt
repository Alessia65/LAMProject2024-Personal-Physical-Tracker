package com.example.personalphysicaltracker.listeners

interface StepCounterListener {
    fun onStepCounterDataReceived(data: String)
}