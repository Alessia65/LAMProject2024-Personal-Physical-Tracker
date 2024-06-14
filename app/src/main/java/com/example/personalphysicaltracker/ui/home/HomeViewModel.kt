package com.example.personalphysicaltracker.ui.home

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.mvvm_todolist.TrackingRepository
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.Activity
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel
    private var selectedActivity: Activity? = null

    private var startTime: String = ""
    private var endTime: String = ""
    private var duration: Long = 0

    fun initializeModel(activity: FragmentActivity?, vmso: ViewModelStoreOwner){
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(vmso, viewModelFactory).get(ActivityViewModel::class.java)

    }

    fun startSelectedActivity(activity: Activity, listener: AccelerometerListener) {
        setSelectedActivity(activity, listener)
        startTime = getCurrentTime()

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
        selectedActivity?.stopActivity()
        endTime = getCurrentTime()
        duration = calculateDuration(startTime, endTime)
        saveActivityData()

    }

    private fun calculateDuration(start: String, end: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        return endTime - startTime
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