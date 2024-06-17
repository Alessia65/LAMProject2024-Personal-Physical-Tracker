package com.example.personalphysicaltracker.activities

import android.content.Context
import android.util.Log
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.WalkingActivityEntity
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WalkingActivity : PhysicalActivity() {

    private var steps: Long = 0


    override fun getActivityName(): String {
        return "Walking"
    }

    fun setActivitySteps(s: Long){
        steps = s
    }

    override suspend fun saveInDb() {
        val activityEntity = ActivityEntity(
            activityType = "Walking",
            date = date,
            timeStart = start,
            timeFinish = end,
            duration = duration
        )

        withContext(Dispatchers.IO) {
            // Inserisci l'attività e attendi il completamento
            activityViewModel.insertActivityEntity(activityEntity)
            Log.d("ATTIVITA INSERITA", "ho inserito l'attività ")

        }

        // Attendi che l'inserimento sia completato


        // Recupera l'ID dell'ultima attività di tipo "Walking" (deve essere una funzione sospesa)
        val id = activityViewModel.getLastWalkingId()

        Log.d("ATTIVITA INSERITA", "ho inserito walking activity con id: $id ")

        activityViewModel.insertWalkingActivityEntity(id, steps)
    }


}
