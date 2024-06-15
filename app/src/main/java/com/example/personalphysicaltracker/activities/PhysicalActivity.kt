package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log

open class PhysicalActivity(context: Context) : PhysicalActivityInterface {

    private lateinit var sensorHandler: SensorHandler

    init {
        // Inizializzazione del SensorHandler nel costruttore
        sensorHandler = SensorHandler(context)
    }

    // Metodo per registrare il listener dell'accelerometro
    fun registerAccelerometerListener(listener: AccelerometerListener) {
        sensorHandler.registerAccelerometerListener(listener)
    }

    override fun getActivityName(): String {
        return "Generic Activity"
    }

    override fun startAccelerometer() {
        sensorHandler.startAccelerometer()
        Log.d("Activity", "Activity started")
    }

    override fun stopActivity() {
        sensorHandler.stopAccelerometer()
        Log.d("Activity", "Activity stopped")
    }
}
