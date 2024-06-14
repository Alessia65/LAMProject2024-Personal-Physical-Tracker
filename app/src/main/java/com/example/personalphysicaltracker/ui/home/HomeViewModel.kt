package com.example.personalphysicaltracker.ui.home

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.mvvm_todolist.TrackingRepository
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.Activity
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel
    private var selectedActivity: Activity? = null

    private var startTime: String = ""
    private var endTime: String = ""
    private var duration: Long = 0
    private val _dailyTime = MutableLiveData<List<Long?>>(listOf(null, null, null))
    val dailyTime: LiveData<List<Long?>>
        get() = _dailyTime


    fun initializeModel(activity: FragmentActivity?, vmso: ViewModelStoreOwner){
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(vmso, viewModelFactory).get(ActivityViewModel::class.java)

        // Eseguire la query per ottenere la somma delle durate per le 3 attività
        updateDailyTimeFromDatabase()
    }

    fun updateDailyTimeFromDatabase() {
        viewModelScope.launch {
            val walkingDuration =
                getTotalDurationByActivityType("WalkingActivity")
            val drivingDuration =
                getTotalDurationByActivityType("DrivingActivity")
            val standingDuration =
                getTotalDurationByActivityType("StandingActivity")

            _dailyTime.value = listOf(walkingDuration, drivingDuration, standingDuration)
            Log.d("dailyTime", "walking: $walkingDuration, driving: $drivingDuration, standing: $standingDuration") //Corretti

        }
    }

    private suspend fun getTotalDurationByActivityType(activityType: String): Long {
        return withContext(Dispatchers.IO) {
            activityViewModel.getTotalDurationByActivityType(activityType)
        }
    }



    fun startSelectedActivity(activity: Activity, listener: AccelerometerListener) {
        setSelectedActivity(activity, listener)
        startTime = getCurrentTime()
        if (startTime.isEmpty()) {
            throw IllegalStateException("startTime can't be empty")
        }

    }

    private fun setSelectedActivity(activity: Activity, listener: AccelerometerListener){
        if (activity is WalkingActivity){
            selectedActivity = activity as WalkingActivity

        } else if (activity is DrivingActivity){
            selectedActivity = activity as DrivingActivity


        } else if (activity is StandingActivity){
            selectedActivity = activity as StandingActivity

        }

        selectedActivity?.registerAccelerometerListener(listener)
        selectedActivity?.startSensor()
    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }


    fun stopSelectedActivity(){

        endTime = getCurrentTime()
        Log.d("HomeViewModel", "startTime: $startTime, endTime: $endTime")

        if (endTime.isEmpty()) {
            throw IllegalStateException("endTime can't be empty")
        }
        duration = calculateDuration(startTime, endTime)
        saveActivityData()
        updateDailyValues()
        selectedActivity?.stopActivity()

    }

    private fun updateDailyValues() {
        viewModelScope.launch {
            var type = selectedActivity?.javaClass?.simpleName ?: "Unknown"
            when (type) {
                "WalkingActivity" -> setDailyTime(0, duration)
                "DrivingActivity" -> setDailyTime(1, duration)
                "StandingActivity" -> setDailyTime(2, duration)
                else -> { /* Gestione per tipi di attività non previsti */ }
            }
        }

    }

    private fun setDailyTime(index: Int, value: Long) {
        viewModelScope.launch {
            _dailyTime.value?.let { currentDailyTime ->
                val updatedList = currentDailyTime.toMutableList()
                if (index in updatedList.indices) {
                    // Somma il nuovo valore al valore corrente
                    val currentValue = updatedList[index] ?: 0L
                    updatedList[index] = currentValue + value
                    _dailyTime.postValue(updatedList)
                } else {
                    // Gestione dell'errore o avviso se l'indice non è valido
                    // Puoi aggiungere un log o una notifica qui se necessario
                }
            }
        }
    }







    private fun calculateDuration(start: String, end: String): Long {
        if (start.isEmpty() || end.isEmpty()) {
            throw IllegalArgumentException("dates can't be empty")
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        return (endTime - startTime)/ 1000
    }

    private fun saveActivityData() {
        val activityEntity = ActivityEntity(
            activityType = selectedActivity?.javaClass?.simpleName ?: "Unknown",
            dateStart = startTime,
            dateFinish = endTime,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }

    fun destoryActivity() {
        selectedActivity?.stopActivity()
    }


}