package com.example.personalphysicaltracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.personalphysicaltracker.activities.ActivityType


// Classe per la gestione dei sensori
class AccelerometerSensorHandler(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometerListener: AccelerometerListener? = null
    private var activityType: ActivityType? = null

    companion object {

        @Volatile
        private var instance: AccelerometerSensorHandler? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: AccelerometerSensorHandler(context).also { instance = it }
            }
    }

    // Metodo per registrare il listener dell'accelerometro
    fun registerAccelerometerListener(listener: AccelerometerListener) {
        accelerometerListener = listener
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    // Metodo per avviare il sensore dell'accelerometro
    fun startAccelerometer(activityType: ActivityType) {
        this.activityType = activityType
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerSensor.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d("Accelerometer", "Sensor started")

    }

    // Metodo per fermare il sensore dell'accelerometro
    fun stopAccelerometer() {
        activityType = null
        sensorManager.unregisterListener(this)
        Log.d("Accelerometer", "Sensor stopped")

    }

    // Metodo richiamato quando i dati del sensore cambiano
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val accelX = it.values[0]
                val accelY = it.values[1]
                val accelZ = it.values[2]
                val message =
                        "${String.format("%.2f", accelX)};${String.format("%.2f", accelY)};${String.format("%.2f", accelZ)}"
                // Notifica il listener dell'accelerometro con i nuovi dati
                accelerometerListener?.onAccelerometerDataReceived(message)
            }
        }
    }


    // Metodo richiamato quando cambia la precisione del sensore (non utilizzato in questo esempio)
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementazione specifica, se necessaria
    }

    fun isActive(): ActivityType?{
        return activityType
    }
}
