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
    private var totalSteps: Long = 0 //fatti ora

    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    private val _dailySteps = MutableLiveData<Long>()
    val dailySteps: LiveData<Long>
        get() = _dailySteps

    private val _actualSteps = MutableLiveData<Long>() //actual steps + passi totali
    val actualSteps: LiveData<Long>
        get() = _actualSteps

    public var stepCounterOn = false
    public var stepCounterWithAcc = false

    suspend fun initialize(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, context: Context) {
        if (!this::activityDBViewModel.isInitialized) {
            val application = requireNotNull(activity).application
            val repository = TrackingRepository(application)
            val viewModelFactory = ActivityViewModelFactory(repository)
            activityDBViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
        }

        accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(context)
        stepCounterSensorHandler = StepCounterSensorHandler.getInstance(context)
        // Load daily data from the database at startup
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
        Log.d("dailyTime", "walking: $walkingDuration, driving: $drivingDuration, standing: $standingDuration, unknown: $unknownDuration")

        val totalSteps = getTotalStepsFromToday()
        _dailySteps.postValue(totalSteps)
        Log.d("dailySteps", "steps: $totalSteps")
    }

    private suspend fun getTotalDurationByActivityTypeToday(activityType: ActivityType): Double {
        val today = getCurrentDay()
        var duration = 0.0

        try {
            // Execute the query in the IO thread to get the duration
            duration = withContext(Dispatchers.IO) {
                activityDBViewModel.getTotalDurationByActivityTypeInDay(today, activityType.toString())
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
                activityDBViewModel.getTotalStepsFromToday(today)
            }
        } catch (e: Exception) {
            // Exception handling, such as logging or other types of handling
            Log.e("HomeViewModel", "Exception while fetching duration")
        }

        return totalSteps
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

    suspend fun startSelectedActivity(activity: PhysicalActivity) {
        started = true
        val lastActivity = getLastActivity()
        val currentTime = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())

        if (lastActivity != null && isGapBetweenActivities(lastActivity.timeFinish, currentTime)) {
            saveUnknownActivity(lastActivity.timeFinish, currentTime)
        }

        //Create Activity
        selectedActivity = activity
        selectedActivity.setActivityViewModelVar(activityDBViewModel)

        val typeForLog = activity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Started")
        startSensors()

    }

    private fun startSensors() {
        val activityType = selectedActivity.getActivityTypeName()
        if (activityType == ActivityType.WALKING){
            Log.d("ACTIVITY HANDLER", "RICHIESTA START STEP COUNTER")
            getStepCounterSensorHandler()
        } else {
            Log.d("ACTIVITY HANDLER", "NON SONO UNA WALKING ACTIVITY")
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
            e.printStackTrace()
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

        /*
        val unknownActivityEntity = ActivityEntity(
            activityType = "UNKNOWN",
            date = date,
            timeStart = lastEndTime,
            timeFinish = currentTime,
            duration = duration
        )

        activityDBViewModel.insertActivityEntity(unknownActivity)

         */


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

    suspend fun stopSelectedActivity(isWalkingActivity: Boolean) {
        started = false
        this.isWalkingActivity = isWalkingActivity
        stopStepCounterSensor()
        stopAccelerometerSensor()
        Log.d("CIAO","CIAO")
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(selectedActivity.start) ?: return // Converte startTime in Date

        // Fine del giorno in cui è iniziata l'attività
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
            (selectedActivity as WalkingActivity).setActivitySteps(totalSteps)
        }

        val typeForLog = selectedActivity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Stopped")

        _dailySteps.postValue(_dailySteps.value)
        saveInDb()
        updateDailyValues()
    }

    // Funzione di utilità per ottenere l'inizio della giornata corrente
    private fun getStartOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    // Funzione di utilità per ottenere la fine della giornata corrente
    private fun getEndOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    // Funzione di utilità per creare una nuova attività dello stesso tipo
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
        this.totalSteps = totalSteps
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

    fun startStepCounterSensor(){
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
        //accelerometerSensorHandler.unregisterAllListeners() //TODO: ATTENZIONE

    }

    fun stopStepCounterSensor(){
        Log.d("STEP COUNTER ON", stepCounterOn.toString())
        //if (stepCounterOn) {
        _dailySteps.postValue((_dailySteps.value ?: 0) + totalSteps)
        stepCounterSensorHandler.unregisterListener()
        stepCounterSensorHandler.stopStepCounter()
        stepCounterOn = false
        Log.d("TOTAL STEPS: " ,  (_dailySteps.value).toString())
        //}
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
        dailyTime.value?.let { dailyTimeList ->
            val currentProgressWalking = dailyTimeList[0] ?: 0.0
            val currentProgressDriving = dailyTimeList[1] ?: 0.0
            val currentProgressStanding = dailyTimeList[2] ?: 0.0

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
            stopSelectedActivity(isWalkingActivity)
            Log.d("ACTIVITY HANDLER", "STOPPED, started: $started")

        }
    }


}