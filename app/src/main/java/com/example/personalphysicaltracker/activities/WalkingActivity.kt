package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WalkingActivity(context: Context) : Activity(context) {

    private var sensorJob: Job? = null
    private var startTimeMillis: Long = 0

    override fun getActivityName(): String {
        return "Walking Activity"
    }

    override fun startSensor() {
        super.startSensor()
        startTimeMillis = System.currentTimeMillis()

        sensorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(1000) // Esegui un'operazione ogni secondo
                // Puoi aggiungere qui il codice per eseguire operazioni periodiche
                // che non coinvolgono il thread principale
            }
        }
    }

    override fun stopActivity() {
        super.stopActivity()
        sensorJob?.cancel() // Assicurati di cancellare il job della coroutine quando l'attività si ferma

        // Calcola la durata del timer
        val endTimeMillis = System.currentTimeMillis()
        val durationMillis = endTimeMillis - startTimeMillis
        val durationSeconds = durationMillis / 1000

        Log.d("WalkingActivity", "Walking activity stopped. Duration: $durationSeconds seconds")
    }


}
