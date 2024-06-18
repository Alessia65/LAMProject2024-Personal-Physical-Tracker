package com.example.personalphysicaltracker

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.sensors.AccelerometerListener
import com.example.personalphysicaltracker.sensors.AccelerometerSensorHandler
import com.example.personalphysicaltracker.sensors.StepCounterListener
import com.example.personalphysicaltracker.sensors.StepCounterSensorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class handerCommon  {

/*
    : AccelerometerListener, StepCounterListener
    private lateinit var activityViewModel: ActivityViewModel
    private lateinit var selectedActivity: PhysicalActivity
    private lateinit var accelerometerSensorHandler: AccelerometerSensorHandler
    private lateinit var stepCounterSensorHandler: StepCounterSensorHandler


    private var date: String = ""
    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    private val _dailySteps = MutableLiveData<Long>()
    val dailySteps: LiveData<Long>
        get() = _dailySteps

    private var stepCounterWithAcc = false
    private var stepCounterOn: Boolean = false
    private var accelerometerOn: Boolean = false
    private var isWalkingActivity = false
    private var totalSteps= 0L


    fun requestSensorActive(): Boolean{
        if (stepCounterOn || accelerometerOn || stepCounterWithAcc){
            return true
        } else {
            return false
        }
    }
    // Initialize the ViewModel with the necessary repository for database operations
    fun initializeActivityViewModel(activity: FragmentActivity?, viewmodelstoreowner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewmodelstoreowner, viewModelFactory).get(
            ActivityViewModel::class.java)

        // Load daily data from the database at startup
        updateDailyTimeFromDatabase()
    }

    fun initializeSensors(context: Context){
        accelerometerSensorHandler = AccelerometerSensorHandler(context)
        stepCounterSensorHandler = StepCounterSensorHandler(context)
    }

    // Get the sum of the duration for a specific activity type for the current day
    private suspend fun getTotalDurationByActivityTypeToday(activityType: String): Double {
        val today = getCurrentDay()
        var duration = 0.0

        try {
            // Execute the query in the IO thread to get the duration
            duration = withContext(Dispatchers.IO) {
                activityViewModel.getTotalDurationByActivityTypeInDay(today, activityType)
            }
        } catch (e: Exception) {
            // Exception handling, such as logging or other types of handling
            Log.e("HomeViewModel", "Exception while fetching duration")
        }

        return duration
    }


    private suspend fun getTotalStepsFromToday(): Long{
        var totalSteps = 0L
        val today = getCurrentDay()
        try {
            // Execute the query in the IO thread to get the duration
            totalSteps = withContext(Dispatchers.IO) {
                activityViewModel.getTotalStepsFromToday(today)
            }
        } catch (e: Exception) {
            // Exception handling, such as logging or other types of handling
            Log.e("HomeViewModel", "Exception while fetching duration")
        }

        return totalSteps
    }

    // Update the daily data (_dailyTime) from the database
    private fun updateDailyTimeFromDatabase() {
        date = getCurrentDay()
        viewModelScope.launch {
            val walkingDuration = getTotalDurationByActivityTypeToday("Walking")
            val drivingDuration = getTotalDurationByActivityTypeToday("Driving")
            val standingDuration = getTotalDurationByActivityTypeToday("Standing")
            val unknownDuration = getUnknownDuration(walkingDuration, drivingDuration, standingDuration)

            // Update the LiveData with the daily data
            _dailyTime.value = listOf(walkingDuration, drivingDuration, standingDuration, unknownDuration)
            Log.d("dailyTime", "walking: $walkingDuration, driving: $drivingDuration, standing: $standingDuration, unknown: $unknownDuration")

            val totalSteps = getTotalStepsFromToday()
            _dailySteps.value = totalSteps
            Log.d("dailySteps", "steps: $totalSteps")
        }
    }

    // Calculate the duration of an unknown activity among the known activities
    private fun getUnknownDuration(walkingDuration: Double, drivingDuration: Double, standingDuration: Double): Double {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfDaySeconds = calendar.timeInMillis / 1000
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val totalKnownDuration = walkingDuration + drivingDuration + standingDuration
        val unknownTime = currentTimeSeconds - startOfDaySeconds - totalKnownDuration

        return if (unknownTime > 0) unknownTime.toDouble() else 0.0
    }

    // Start a selected activity
    fun startSelectedActivity(activity: PhysicalActivity) {
        viewModelScope.launch(Dispatchers.IO) {

            val lastActivity = getLastActivity()
            val currentTime = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(
                Date()
            )

            if (lastActivity != null && isGapBetweenActivities(lastActivity.timeFinish, currentTime)) {
                saveUnknownActivity(lastActivity.timeFinish, currentTime)
            }

            //Create Activity
            selectedActivity = activity
            selectedActivity.setActivityViewModelVar(activityViewModel)
        }
    }

    // Get the last saved activity
    private suspend fun getLastActivity(): ActivityEntity? {
        return withContext(Dispatchers.IO) {
            activityViewModel.getLastActivity()
        }
    }

    // Check if there is a gap between activities based on timestamps
    private fun isGapBetweenActivities(lastEndTime: String, currentTime: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val lastEndTimeDate = dateFormat.parse(lastEndTime)
            val currentTimeDate = dateFormat.parse(currentTime)

            lastEndTimeDate != null && currentTimeDate != null && lastEndTimeDate < currentTimeDate
        } catch (e: ParseException) {
            e.printStackTrace()
            false
        }
    }

    // Save an unknown activity
    private fun saveUnknownActivity(lastEndTime: String, currentTime: String) {
        val duration = calculateDuration(lastEndTime, currentTime)
        val unknownActivity = ActivityEntity(
            activityType = "Unknown",
            date = date,
            timeStart = lastEndTime,
            timeFinish = currentTime,
            duration = duration
        )
        viewModelScope.launch(Dispatchers.IO) {
            activityViewModel.insertActivityEntity(unknownActivity)
        }
    }


    // Get the current date in the specified format
    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // Stop the selected activity, calculate the duration, and update the daily values
    fun stopSelectedActivity(steps: Long, isWalkingActivity: Boolean) {
        selectedActivity.setFinishTime()
        selectedActivity.calculateDuration()

        if (isWalkingActivity){
            (selectedActivity as WalkingActivity).setActivitySteps(steps)
        }
        saveInDb()
        updateDailyValues()
    }

    private fun saveInDb(){
        Log.d("INSERIMENTO", "Sto per inserire")

        viewModelScope.launch {
            if (selectedActivity is WalkingActivity){
                Log.d("INSERIMENTO", "Sto per inserire W")
                (selectedActivity as WalkingActivity).saveInDb()
            } else if (selectedActivity is DrivingActivity){
                Log.d("INSERIMENTO", "Sto per inserire D")
                (selectedActivity as DrivingActivity).saveInDb()
            } else if (selectedActivity is StandingActivity){
                Log.d("INSERIMENTO", "Sto per inserire S")
                (selectedActivity as StandingActivity).saveInDb()
            } else {
                Log.d("INSERIMENTO", "Sto per inserire UNKNOWN")
                selectedActivity.saveInDb()
            }
        }

    }
    // Update the daily values based on the selected activity
    private fun updateDailyValues() {
        /*
        val d = selectedActivity.duration
        Log.d("CHIUSURA ACTIVITY", "calcolo duration: $d")

         */
        viewModelScope.launch {
            updateDailyTimeFromDatabase()
        }
    }



    // Calculate the duration between two timestamps in the specified format
    private fun calculateDuration(start: String, end: String): Double {
        if (start.isEmpty() || end.isEmpty()) {
            throw IllegalArgumentException("dates can't be empty")
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        return (endTime - startTime).toDouble() / 1000
    }

    fun startAccelerometerSensor(){
        accelerometerSensorHandler.registerAccelerometerListener(this)
        accelerometerSensorHandler.startAccelerometer()
        accelerometerOn = true
    }

    fun startStepCounterSensor(): Boolean{
        stepCounterSensorHandler.registerStepCounterListener(this)
        var presenceStepCounterSensor = stepCounterSensorHandler.startStepCounter()

        if (presenceStepCounterSensor){
            stepCounterOn = true
        }
        return presenceStepCounterSensor
    }

    fun stopAccelerometerSensor(){
        accelerometerSensorHandler.unregisterListener()
        accelerometerSensorHandler.stopAccelerometer()
        accelerometerOn = false
    }

    fun stopStepCounterSensor(){
        if (stepCounterOn) {
            stepCounterSensorHandler.unregisterListener()
            stepCounterSensorHandler.stopStepCounter()
            stepCounterOn = false
        }
    }

    fun setAttributes(stepCounterOnTemp: Boolean, stepCounterWithAccTemp: Boolean){
        stepCounterOn = stepCounterOnTemp
        stepCounterWithAcc = stepCounterWithAccTemp
    }

    override fun onAccelerometerDataReceived(data: String) {



        if (stepCounterWithAcc) {
            registerStep(data)
        }
        /*
        homeViewModel.dailyTime.value?.let { dailyTimeList ->
            val currentProgressWalking = dailyTimeList[0]?.toDouble() ?: 0.0
            val currentProgressDriving = dailyTimeList[1]?.toDouble() ?: 0.0
            val currentProgressStanding = dailyTimeList[2]?.toDouble() ?: 0.0
            //Log.d("CHIUSURA ACTIVITY", "valori correnti: $currentProgressWalking, $currentProgressDriving, $currentProgressStanding")
            updateProgressBarWalking(currentProgressWalking)
            updateProgressBarDriving(currentProgressDriving)
            updateProgressBarStanding(currentProgressStanding)
        }

         */
    }

    fun registerStep(data: String) {
        var step = stepCounterSensorHandler.registerStepWithAccelerometer(data)
        onStepCounterDataReceived(step.toString())
    }

    override fun onStepCounterDataReceived(data: String) {
        /*
        requireActivity().runOnUiThread {
            Log.d("Step counter", "Aggiornamento ottenuto")
            stepsText.text = "Current steps: $data"
            totalSteps = data.toLong()

        }

         */
    }

 */
}