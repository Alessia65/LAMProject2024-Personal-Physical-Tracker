package com.example.personalphysicaltracker.ui.home

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.example.personalphysicaltracker.activities.*
import com.example.personalphysicaltracker.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

//Comunicazione tra HomeFragment e Altre classi
class HomeViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel
    private lateinit var selectedActivity: PhysicalActivity


    private var date: String = ""
    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    private val _dailySteps = MutableLiveData<Long>()
    val dailySteps: LiveData<Long>
        get() = _dailySteps


    // Initialize the ViewModel with the necessary repository for database operations
    fun initializeActivityViewModel(activity: FragmentActivity?, viewmodelstoreowner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewmodelstoreowner, viewModelFactory).get(ActivityViewModel::class.java)

        // Load daily data from the database at startup
        updateDailyTimeFromDatabase()
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
            val currentTime = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())

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


}
