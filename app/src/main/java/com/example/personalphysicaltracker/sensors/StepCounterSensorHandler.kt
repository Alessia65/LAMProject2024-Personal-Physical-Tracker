package com.example.personalphysicaltracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class StepCounterSensorHandler(context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterListener: StepCounterListener? = null
    private var presenceOnDevice: Boolean = false
    private var running: Boolean = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var magnitude = 0f
    private var magnitudePreviousStep = 0f
    private var firstChange = true
    private var thresholdMagnitudeDelta = 1.0f
    companion object {

        @Volatile
        private var instance: StepCounterSensorHandler? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: StepCounterSensorHandler(context).also { instance = it }
            }
    }

    fun registerStepCounterListener(listener: StepCounterListener) {
        stepCounterListener = listener
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    fun startStepCounter(): Boolean {
        firstChange = true
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        return if (stepCounterSensor!= null) {
            presenceOnDevice = true
            stepCounterSensor.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            Log.d("Step Counter", "Sensor started")
            running = true
            true
        } else {
            false
        }


    }

    fun stopStepCounter() {
        //if (presenceOnDevice) { //TODO: controllare
        if (running) {
            sensorManager.unregisterListener(this)
            Log.d("Step Counter", "Sensor stopped")
            running = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("Step Counter", "Change on Sensor")
        if (presenceOnDevice) {

            Log.d("Step Counter", "Here")

            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER && running) {
                    totalSteps = event.values[0]

                    if (firstChange){
                        firstChange = false
                        previousTotalSteps = totalSteps
                        Log.d("Step Counter", "First Change, steps: $totalSteps")
                    }


                    val currentSteps = totalSteps.toLong() - previousTotalSteps.toLong()
                    Log.d("Step Counter", "Current steps: $currentSteps, totalSteps: $totalSteps, previousSteps: $previousTotalSteps")

                    stepCounterListener?.onStepCounterDataReceived(currentSteps.toString())

                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necessary
    }

    //TODO: approximate better
    fun registerStepWithAccelerometer(data: String): Long{
        val split = data.split(";")
        if (split.size >= 3) {
            val x = split[0].toFloat()
            val y = split[1].toFloat()
            val z = split[2].toFloat()

            magnitude = sqrt(x * x + y * y + z * z)

            if (firstChange) {
                totalSteps = 0f
                magnitudePreviousStep = magnitude
                firstChange = false
            }

            val magnitudeDelta = magnitude - magnitudePreviousStep
            magnitudePreviousStep = magnitude

            if (magnitudeDelta > thresholdMagnitudeDelta) {
                totalSteps++
            }

            return totalSteps.toLong()

        } else {
            Log.e("Step Counter", "Invalid data format: $data")
            return 0
        }

    }

    fun isActive(): Boolean{
        return running
    }
}