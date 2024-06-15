package com.example.personalphysicaltracker.ui.home

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.Activity
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel
    private var selectedActivity: Activity? = null

    private var startTime: String = ""
    private var endTime: String = ""
    private var duration: Double = 0.0
    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    fun initializeModel(activity: FragmentActivity?, viewmodelstoreowner: ViewModelStoreOwner){
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewmodelstoreowner, viewModelFactory).get(ActivityViewModel::class.java)

        // Eseguire la query per ottenere la somma delle durate per le 3 attività
        updateDailyTimeFromDatabase()
    }

    private fun updateDailyTimeFromDatabase() {
        viewModelScope.launch {
            val walkingDuration =
                getTotalDurationByActivityType("WalkingActivity")
            val drivingDuration =
                getTotalDurationByActivityType("DrivingActivity")
            val standingDuration =
                getTotalDurationByActivityType("StandingActivity")
            val unknownDuration =
                getUnknownDuration(walkingDuration, drivingDuration, standingDuration)

            _dailyTime.value = listOf(walkingDuration, drivingDuration, standingDuration, unknownDuration)
            Log.d("dailyTime", "walking: $walkingDuration, driving: $drivingDuration, standing: $standingDuration, unknown: $unknownDuration") //Corretti

        }
    }

    private fun getUnknownDuration(walkingDuration: Double, drivingDuration: Double, standingDuration: Double): Double {
        // Calcola l'inizio del giorno in secondi
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDaySeconds = calendar.timeInMillis / 1000

        // Ottieni l'ora corrente in secondi
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        // Calcola la durata totale delle attività conosciute
        val totalKnownDuration = walkingDuration + drivingDuration + standingDuration

        // Calcola il tempo sconosciuto tra l'inizio del giorno e l'ora corrente
        val unknownTime = currentTimeSeconds - startOfDaySeconds - totalKnownDuration

        // Log per verificare i valori calcolati
        Log.d("getUnknownDuration", "currentTime: $currentTimeSeconds, startOfDay: $startOfDaySeconds, totalKnownDuration: $totalKnownDuration, unknownTime: $unknownTime")

        // Assicura che unknownTime non sia negativo
        return if (unknownTime > 0) unknownTime else 0.0
    }

    private suspend fun getTotalDurationByActivityType(activityType: String): Double {
        return withContext(Dispatchers.IO) {
            activityViewModel.getTotalDurationByActivityType(activityType)
        }
    }

    fun startSelectedActivity(activity: Activity, listener: AccelerometerListener) {
        viewModelScope.launch {
            val lastActivity = getLastActivity()
            val currentTime = getCurrentTime()

            if (lastActivity != null) {
                val lastEndTime = lastActivity.dateFinish
                if (isGapBetweenActivities(lastEndTime, currentTime)) {
                    saveUnknownActivity(lastEndTime, currentTime)
                }
            }
            setSelectedActivity(activity, listener)
            startTime = currentTime
            if (startTime.isEmpty()) {
                throw IllegalStateException("startTime can't be empty")
            }
        }
    }

    private suspend fun getLastActivity(): ActivityEntity? {
        return withContext(Dispatchers.IO) {
            activityViewModel.getLastActivity()
        }
    }

    private fun isGapBetweenActivities(lastEndTime: String, currentTime: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val lastEnd = dateFormat.parse(lastEndTime)?.time ?: 0L
        val currentStart = dateFormat.parse(currentTime)?.time ?: 0L
        return currentStart > lastEnd
    }

    private fun saveUnknownActivity(lastEndTime: String, currentTime: String) {
        val duration = calculateDuration(lastEndTime, currentTime)
        val unknownActivity = ActivityEntity(
            activityType = "Unknown",
            dateStart = lastEndTime,
            dateFinish = currentTime,
            duration = duration
        )
        viewModelScope.launch {
            activityViewModel.insertActivityEntity(unknownActivity)
        }
    }

    private fun setSelectedActivity(activity: Activity, listener: AccelerometerListener){
        when (activity) {
            is WalkingActivity -> selectedActivity = activity
            is DrivingActivity -> selectedActivity = activity
            is StandingActivity -> selectedActivity = activity
        }
        selectedActivity?.registerAccelerometerListener(listener)
        selectedActivity?.startSensor()
    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun stopSelectedActivity() {
        endTime = getCurrentTime()
        Log.d("HomeViewModel", "startTime: $startTime, endTime: $endTime")

        if (endTime.isEmpty()) {
            throw IllegalStateException("endTime can't be empty")
        }
        duration = calculateDuration(startTime, endTime)
        saveActivityData(selectedActivity?.javaClass?.simpleName ?: "Unknown")
        updateDailyValues()
        selectedActivity?.stopActivity()
    }


    private fun updateDailyValues() {
        viewModelScope.launch {
            when (selectedActivity?.javaClass?.simpleName ?: "Unknown") {
                "WalkingActivity" -> setDailyTime(0, duration)
                "DrivingActivity" -> setDailyTime(1, duration)
                "StandingActivity" -> setDailyTime(2, duration)
                else -> { /* Gestione per tipi di attività non previsti */ }
            }
        }
    }

    private fun setDailyTime(index: Int, value: Double) {
        viewModelScope.launch {
            _dailyTime.value?.let { currentDailyTime ->
                val updatedList = currentDailyTime.toMutableList()
                if (index in updatedList.indices) {
                    // Somma il nuovo valore al valore corrente
                    val currentValue = updatedList[index] ?: 0.0
                    updatedList[index] = currentValue + value
                    _dailyTime.postValue(updatedList)
                } else {
                    // Gestione dell'errore o avviso se l'indice non è valido
                    // Puoi aggiungere un log o una notifica qui se necessario
                }
            }
        }
    }

    private fun calculateDuration(start: String, end: String): Double {
        if (start.isEmpty() || end.isEmpty()) {
            throw IllegalArgumentException("dates can't be empty")
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0.0
        val endTime = dateFormat.parse(end)?.time ?: 0.0
        return (endTime.toDouble() - startTime.toDouble()) / 1000
    }

    private fun saveActivityData(activityType: String) {
        val activityEntity = ActivityEntity(
            activityType = activityType,
            dateStart = startTime,
            dateFinish = endTime,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }

    fun destroyActivity() {
        selectedActivity?.stopActivity()
    }
}
