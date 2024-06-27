package com.example.personalphysicaltracker.activities

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ActivityHandler {

    private lateinit var activityViewModel: ActivityViewModel
    private lateinit var selectedActivity: PhysicalActivity

    private var date: String = ""
    private val _dailyTime = MutableLiveData<List<Double?>>(listOf(null, null, null, null))
    val dailyTime: LiveData<List<Double?>>
        get() = _dailyTime

    private val _dailySteps = MutableLiveData<Long>()
    val dailySteps: LiveData<Long>
        get() = _dailySteps


    suspend fun initialize(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {
        if (!this::activityViewModel.isInitialized) {
            val application = requireNotNull(activity).application
            val repository = TrackingRepository(application)
            val viewModelFactory = ActivityViewModelFactory(repository)
            activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityViewModel::class.java]
        }
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
                activityViewModel.getTotalDurationByActivityTypeInDay(today, activityType.toString())
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
        val lastActivity = getLastActivity()
        val currentTime = (SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())).format(Date())

        if (lastActivity != null && isGapBetweenActivities(lastActivity.timeFinish, currentTime)) {
            saveUnknownActivity(lastActivity.timeFinish, currentTime)
        }

        //Create Activity
        selectedActivity = activity
        selectedActivity.setActivityViewModelVar(activityViewModel)

        val typeForLog = activity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Started")
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
            activityType = "UNKNOWN",
            date = date,
            timeStart = lastEndTime,
            timeFinish = currentTime,
            duration = duration
        )

        activityViewModel.insertActivityEntity(unknownActivity)

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

    suspend fun stopSelectedActivity(steps: Long, isWalkingActivity: Boolean) {
        selectedActivity.setFinishTime()
        selectedActivity.calculateDuration()

        if (isWalkingActivity){
            (selectedActivity as WalkingActivity).setActivitySteps(steps)
        }

        val typeForLog = selectedActivity.getActivityTypeName()
        Log.d("PHYSICAL ACTIVITY", "$typeForLog Activity Stopped")
        saveInDb()
        updateDailyValues()
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


}