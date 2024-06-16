package com.example.personalphysicaltracker.activities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


// Classe per la gestione dei sensori
class SensorHandler(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometerListener: AccelerometerListener? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    }

    // Metodo per registrare il listener dell'accelerometro
    fun registerAccelerometerListener(listener: AccelerometerListener) {
        accelerometerListener = listener
    }

    // Metodo per avviare il sensore dell'accelerometro
    fun startAccelerometer() {
        val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Metodo per fermare il sensore dell'accelerometro
    fun stopAccelerometer() {
        sensorManager?.unregisterListener(this)
    }

    // Metodo richiamato quando i dati del sensore cambiano
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val accelX = it.values[0]
                val accelY = it.values[1]
                val accelZ = it.values[2]
                val message = "Dati: \n" +
                        "Acc. X: ${String.format("%.2f", accelX)} \n" +
                        "Acc. Y: ${String.format("%.2f", accelY)} \n" +
                        "Acc. Z: ${String.format("%.2f", accelZ)}"

                // Notifica il listener dell'accelerometro con i nuovi dati
                accelerometerListener?.onAccelerometerDataReceived(message)
            }
        }
    }

    // Metodo richiamato quando cambia la precisione del sensore (non utilizzato in questo esempio)
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementazione specifica, se necessaria
    }
}
