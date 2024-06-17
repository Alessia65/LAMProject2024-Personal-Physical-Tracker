package com.example.personalphysicaltracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepCounterSensorHandler(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterListener: StepCounterListener? = null
    private var presenceOnDevice: Boolean = false
    private var running: Boolean = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var firstChange = true

    // Metodo per registrare il listener dell'accelerometro
    fun registerStepCounterListener(listener: StepCounterListener) {
        stepCounterListener = listener
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    // Metodo per avviare il sensore dell'accelerometro
    fun startStepCounter() {
        firstChange = true
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor!= null) {
            presenceOnDevice = true
            stepCounterSensor.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            Log.d("Step Counter", "Sensor started")
            running = true
        }

    }

    // Metodo per fermare il sensore dell'accelerometro
    fun stopStepCounter() {
        if (presenceOnDevice) {
            sensorManager.unregisterListener(this)
            Log.d("Step Counter", "Sensor stopped")
            running = false
        }
    }

    // Metodo richiamato quando i dati del sensore cambiano
    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("Step Counter", "Change on Sensor")
        if (presenceOnDevice) {

            Log.d("Step Counter", "Here")

            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER && running) {
                    totalSteps = event.values[0]

                    if (firstChange){
                        firstChange = false
                        previousTotalSteps = totalSteps //Altrimenti sono uguali ai passi fatti dall'ultimo riavvio del dispositivo
                        Log.d("Step Counter", "First Change, steps: $totalSteps")
                    }


                    val currentSteps = totalSteps.toLong() - previousTotalSteps.toLong()
                    Log.d("Step Counter", "Current steps: $currentSteps, totalSteps: $totalSteps, previousSteps: $previousTotalSteps")

                    stepCounterListener?.onStepCounterDataReceived(currentSteps.toString())

                }
            }
        }
    }

    // Metodo richiamato quando cambia la precisione del sensore (non utilizzato)
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementazione specifica, se necessaria
    }
}