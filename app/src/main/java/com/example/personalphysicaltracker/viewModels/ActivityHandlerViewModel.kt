package com.example.personalphysicaltracker.viewModels

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.handlers.AccelerometerSensorHandler
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.StepCounterSensorHandler
import com.example.personalphysicaltracker.listeners.AccelerometerListener
import com.example.personalphysicaltracker.listeners.StepCounterListener
import com.example.personalphysicaltracker.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActivityHandlerViewModel:  ViewModel(), AccelerometerListener, StepCounterListener {

    private lateinit var activityDBViewModel: ActivityDBViewModel
    private lateinit var selectedActivity: PhysicalActivity
    private lateinit var accelerometerSensorHandler: AccelerometerSensorHandler
    private lateinit var stepCounterSensorHandler: StepCounterSensorHandler
    private var isWalkingActivity = false
    private var started = false

    private var date: String = ""
    private var totalStepsDone: Long = 0

    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    private val _dailySteps = MutableLiveData<Long>()
    val dailySteps: LiveData<Long>
        get() = _dailySteps

    private val _actualSteps = MutableLiveData<Long>()
    val actualSteps: LiveData<Long>
        get() = _actualSteps

    private var stepCounterOn = false
    private var stepCounterWithAcc = false

    suspend fun initialize(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, context: Context) {
        if (!this::activityDBViewModel.isInitialized) {
            val application = requireNotNull(activity).application
            val repository = TrackingRepository(application)
            val viewModelFactory = ActivityViewModelFactory(repository)
            activityDBViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
        }

        accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(context)
        stepCounterSensorHandler = StepCounterSensorHandler.getInstance(context)

        updateDailyTimeFromDatabase()

    }

    private suspend fun updateDailyTimeFromDatabase() {
        date = getCurrentDay()

        val walkingDuration = getTotalDurationByActivityTypeToday(ActivityType.WALKING)
        val drivingDuration = getTotalDurationByActivityTypeToday(ActivityType.DRIVING)
        val standingDuration = getTotalDurationByActivityTypeToday(ActivityType.STANDING)
        val unknownDuration = getUnknownDuration(walkingDuration, drivingDuration, standingDuration)

        // Update the LiveData with the daily data
        _dailyTime.postValue(listOf(walkingDuration, drivingDuration, standingDuration, unknownDuration))
        Log.d("ACTIVITY HANDLER VIEW MODEL", "Daily values: walking: $walkingDuration, driving: $drivingDuration, standing: $standingDuration, unknown: $unknownDuration")

        val dailyTotalSteps = getTotalStepsFromToday()
        _dailySteps.postValue(dailyTotalSteps)
        Log.d("ACTIVITY HANDLER VIEW MODEL", "daily steps: $dailyTotalSteps")
    }

    private suspend fun getTotalDurationByActivityTypeToday(activityType: ActivityType): Double {
        val today = getCurrentDay()
        var duration = 0.0

        try {
            duration = withContext(Dispatchers.IO) {
                activityDBViewModel.getTotalDurationByActivityTypeInDay(today, activityType.toString())
            }
        } catch (e: Exception) {
            Log.e("ACTIVITY HANDLER VIEW MODEL", "Exception while fetching duration: an activity type may not have valid values")
        }

        return duration
    }

    private suspend fun getTotalStepsFromToday(): Long{
        var dailyTotalSteps = 0L
        val today = getCurrentDay()
        try {
            dailyTotalSteps = withContext(Dispatchers.IO) {
                activityDBViewModel.getTotalStepsFromToday(today)
            }
        } catch (e: Exception) {
            Log.e("ACTIVITY HANDLER VIEW MODEL", "Exception while getting daily total steps value")
        }

        return dailyTotalSteps
    }

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

        return if (unknownTime > 0) unknownTime else 0.0
    }

    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    suspend fun startSelectedActivity(activity: PhysicalActivity, isBackground: Boolean): PhysicalActivity {
        started = true
        val lastActivity = getLastActivity()
        val currentTime = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())

        if (lastActivity != null && isGapBetweenActivities(lastActivity.timeFinish, currentTime)) {
            saveUnknownActivity(lastActivity.timeFinish, currentTime)
        }


        selectedActivity = activity
        selectedActivity.setActivityViewModelVar(activityDBViewModel)
        if (isBackground) {
            ActivityTransitionHandler.setCurrentActivity(selectedActivity)
        }
        val typeForLog = activity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Started")

        return selectedActivity
    }

     fun startSensors() {
        val activityType = selectedActivity.getActivityTypeName()
        if (activityType == ActivityType.WALKING){
            getStepCounterSensorHandler()
        }
        startAccelerometerSensor(activityType)
    }

    // Get the last saved activity
    private suspend fun getLastActivity(): ActivityEntity? {
        return withContext(Dispatchers.IO) {
            activityDBViewModel.getLastActivity()
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
            Log.d("ACTIVITY HANDLER VIEW MODEL", "Parse Exception occurred")
            false
        }
    }


    // Save an unknown activity
    private suspend fun saveUnknownActivity(lastEndTime: String, currentTime: String) {
        val unknownActivity = PhysicalActivity()
        unknownActivity.setActivityViewModelVar(activityDBViewModel)
        val duration = calculateDuration(lastEndTime, currentTime)
        unknownActivity.date = date
        unknownActivity.start = lastEndTime
        unknownActivity.end = currentTime
        unknownActivity.duration = duration
        unknownActivity.saveInDb()
    }

    private fun calculateDuration(start: String, end: String): Double {
        if (start.isEmpty() || end.isEmpty()) {
            Log.e("ACTIVITY HANDLER VIEW MODEL","dates can't be empty")
            return 0.0
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        return (endTime - startTime).toDouble() / 1000
    }

    suspend fun stopSelectedActivity(isWalkingActivity: Boolean, isBackground: Boolean) {
        if (!::selectedActivity.isInitialized && isBackground) {
            val temp = ActivityTransitionHandler.getCurrentActivity()
            if (temp!=null){
                selectedActivity = temp
            } else {
                return
            }
        }
        started = false
        this.isWalkingActivity = isWalkingActivity


        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(selectedActivity.start) ?: return

        val endOfDay = getEndOfToday(startTime)

        if (startTime.before(getStartOfToday(currentTime))) {
            val endOfDayString = dateFormat.format(endOfDay)
            selectedActivity.setFinishTimeWithString(endOfDayString)
            selectedActivity.calculateDuration()
            saveInDb()

            val newActivity = createNewActivityOfSameType(selectedActivity)
            selectedActivity = newActivity
            selectedActivity.setActivityViewModelVar(activityDBViewModel)

            val startOfNextDay = Calendar.getInstance().apply {
                time = endOfDay
                add(Calendar.SECOND, 1)
            }.time

            val startOfDayString = dateFormat.format(startOfNextDay)
            selectedActivity.setStartTimeWIthString(startOfDayString)

        }

        selectedActivity.setFinishTime()
        selectedActivity.calculateDuration()

        if (isWalkingActivity){
            (selectedActivity as WalkingActivity).setActivitySteps(totalStepsDone)
        }

        val typeForLog = selectedActivity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Stopped")

        _dailySteps.postValue(_dailySteps.value)
        saveInDb()
        updateDailyValues()
    }

    fun stopSensors(){
        stopStepCounterSensor()
        stopAccelerometerSensor()
    }
    private fun getStartOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun createNewActivityOfSameType(activity: PhysicalActivity): PhysicalActivity {
        return when (activity) {
            is WalkingActivity -> WalkingActivity()
            is StandingActivity -> StandingActivity()
            is DrivingActivity -> DrivingActivity()
            else -> PhysicalActivity()
        }
    }


    private suspend fun saveInDb(){

        when (selectedActivity) {
            is WalkingActivity -> {
                (selectedActivity as WalkingActivity).saveInDb()
            }

            is DrivingActivity -> {
                (selectedActivity as DrivingActivity).saveInDb()
            }

            is StandingActivity -> {
                (selectedActivity as StandingActivity).saveInDb()
            }

            else -> {
                selectedActivity.saveInDb()
            }
        }
    }


    // Update the daily values based on the selected activity
    private suspend fun updateDailyValues() {
        updateDailyTimeFromDatabase()
    }

    private fun setSteps(totalSteps: Long) {
        this.totalStepsDone = totalSteps
        _actualSteps.postValue(totalSteps)
    }

    private fun startAccelerometerSensor(activityType: ActivityType){
        accelerometerSensorHandler.registerAccelerometerListener(this)
        accelerometerSensorHandler.startAccelerometer(activityType)
    }


    fun stepCounterActive(): Boolean{
        return stepCounterSensorHandler.isActive()
    }

    fun accelerometerActive(): ActivityType?{
        return accelerometerSensorHandler.isActive()
    }

    private fun startStepCounterSensor(){
        stepCounterSensorHandler.registerStepCounterListener(this)
    }

    fun getStepCounterSensorHandler(): Boolean {
        val temp = stepCounterSensorHandler.startStepCounter()
        if (temp){
            startStepCounterSensor()
            stepCounterOn = true
        }
        return temp
    }

    private fun stopAccelerometerSensor(){
        accelerometerSensorHandler.unregisterListener()
        accelerometerSensorHandler.stopAccelerometer()

    }

    private fun stopStepCounterSensor(){
        _dailySteps.postValue((_dailySteps.value ?: 0) + totalStepsDone)
        stepCounterSensorHandler.unregisterListener()
        stepCounterSensorHandler.stopStepCounter()
        stepCounterOn = false
        Log.d("ACTIVITY HANDLER VIEW MODEL" ,"New daily Steps:  ${(_dailySteps.value)}")
    }

    fun setStepCounterOnValue(value: Boolean){
        stepCounterOn = value
    }

    fun setStepCounterWithAccValue(value: Boolean){
        stepCounterWithAcc = value
    }


    override fun onAccelerometerDataReceived(data: String) {
        if (stepCounterWithAcc) {
            registerStep(data)
        }
    }

    override fun onStepCounterDataReceived(data: String) {
        setSteps(data.toLong())
    }

    private fun registerStep(data: String) {
        val step = stepCounterSensorHandler.registerStepWithAccelerometer(data)
        onStepCounterDataReceived(step.toString())
    }

    suspend fun handleDestroy(context: Context){
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        val  backgroundRecognitionEnabled = sharedPreferencesBackgroundActivities.getBoolean(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false)

        if (!backgroundRecognitionEnabled && started){
            stopSensors()
            stopSelectedActivity(isWalkingActivity, false)
            Log.d("ACTIVITY HANDLER VIEW MODEL", "Activity forced to stop, started value: $started")
        }
    }



}