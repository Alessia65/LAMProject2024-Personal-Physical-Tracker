package com.example.personalphysicaltracker.activities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch



open class Activity(private val context: Context) : ActivityInterface, SensorEventListener {

    private var accelerometerListener: AccelerometerListener? = null
    private lateinit var sensorManager: SensorManager
    private var sensorAcc: Sensor? = null
    private var sensorJob: Job? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // Metodo per registrare il listener
    fun registerAccelerometerListener(listener: AccelerometerListener) {
        accelerometerListener = listener
    }

    override fun getActivityName(): String {
        return "Generic Activity"
    }

    override fun startSensor() {
        sensorAcc?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d("Activity", "Activity started")
        sensorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(1000) // Esegui un'operazione ogni secondo
                // Puoi aggiungere qui il codice per eseguire operazioni periodiche
                // che non coinvolgono il thread principale
            }
        }
    }

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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementazione specifica nelle sottoclassi, se necessario
    }

    fun stopActivity() {
        sensorManager.unregisterListener(this)
        sensorJob?.cancel() // Assicurati di cancellare il job della coroutine quando l'attività si ferma
        Log.d("Activity", "Activity stopped")
    }
}
