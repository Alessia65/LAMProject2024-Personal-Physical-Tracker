package com.example.personalphysicaltracker.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.personalphysicaltracker.activities.ActivityType


class AccelerometerSensorHandler(context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometerListener: AccelerometerListener? = null
    private var activityType: ActivityType? = null

    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AccelerometerSensorHandler? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: AccelerometerSensorHandler(context).also { instance = it }
            }
    }

    fun registerAccelerometerListener(listener: AccelerometerListener) {
        accelerometerListener = listener
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    fun startAccelerometer(activityType: ActivityType) {
        this.activityType = activityType
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerSensor.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d("Accelerometer", "Sensor started")

    }

    fun stopAccelerometer() {
        activityType = null
        sensorManager.unregisterListener(this)
        Log.d("Accelerometer", "Sensor stopped")

    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val accelX = it.values[0]
                val accelY = it.values[1]
                val accelZ = it.values[2]
                val message =
                        "${String.format("%.2f", accelX)};${String.format("%.2f", accelY)};${String.format("%.2f", accelZ)}"
                accelerometerListener?.onAccelerometerDataReceived(message)
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not necessary
    }

    fun isActive(): ActivityType?{
        return activityType
    }
}
