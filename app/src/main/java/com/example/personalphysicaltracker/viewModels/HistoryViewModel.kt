package com.example.personalphysicaltracker.viewModels

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.LocationInfo
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.handlers.ShareHandler
import com.example.personalphysicaltracker.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private var endDate: String = ""
    private var startDate: String = ""
    private var durationAtLocation: Double? = 0.0

    private lateinit var activityDBViewModel: ActivityDBViewModel

    // List to hold activities fetched from the database
    private var activitiesOnDb: List<PhysicalActivity> = emptyList()

    // List to hold activities to be sent or displayed in history fragment
    private var activitiesToSend: List<PhysicalActivity> = emptyList()

    private var locationInfo: List<LocationInfo> = emptyList()
    private val selectedFilters: MutableList<String> = mutableListOf()


    fun initializeActivityViewModel(activity: FragmentActivity?) {
        // Ensure activity is not null
        val viewModelStoreOwner = activity ?: throw IllegalArgumentException("Activity must not be null")

        // Create repository and ViewModelFactory instances
        val application = activity.application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityDBViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
        updateFromDatabase()
    }

    private fun updateFromDatabase(): List<PhysicalActivity> {
        viewModelScope.launch(Dispatchers.IO) {
            activitiesOnDb = activityDBViewModel.getListOfPhysicalActivities()
        }
        return activitiesOnDb
    }

    private fun obtainDates(): List<PhysicalActivity> {
        return activitiesOnDb
    }


    fun obtainActivitiesForTransaction(): List<PhysicalActivity> {
        updateFromDatabase()
        return activitiesToSend
    }

    fun handleSelectedDateRange(startDate: String, endDate: String) {
         this.startDate = startDate
         this.endDate = endDate
         activitiesToSend = obtainDates()

         // Filter activities based on selected date range
         activitiesToSend =  activitiesToSend.filter {
             it.date in startDate..endDate

        }
         activitiesToSend = applyFilters(activitiesToSend)
     }


    fun addFilter(activityType: String): List<PhysicalActivity> {
        if (!selectedFilters.contains(activityType)) {
            selectedFilters.add(activityType)
        }

        Log.d("HISTORY VIEW MODEL", "$activityType FILTER ADDED")
        return applyFilters(activitiesToSend)
    }

    fun removeFilter(activityType: String): List<PhysicalActivity> {
        selectedFilters.remove(activityType)
        Log.d("HISTORY VIEW MODEL", "$activityType FILTER REMOVED")
        return applyFilters(activitiesToSend)
    }

    private fun applyFilters(activities: List<PhysicalActivity>): List<PhysicalActivity> {
        if (selectedFilters.isEmpty()) {
            return activities
        }

        return activities.filter { activity ->
            selectedFilters.any { filter ->
                activity.getActivityTypeName().toString() == filter
            }
        }
    }

    fun clearFilters() {
        selectedFilters.clear()
        Log.d("HISTORY VIEW MODEL", "FILTER CLEARED")
    }

    fun exportActivitiesToCSV(context: Context, activities: List<PhysicalActivity>) {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val enabledLocation = sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false)

        if (enabledLocation){
            viewModelScope.launch(Dispatchers.IO) {
               ShareHandler.exportActivitiesToCSV(context, activities, true, locationInfo)

            }
        } else {
            ShareHandler.exportActivitiesToCSV(context, activities, false, locationInfo)

        }
    }


    fun getTotalPresenceInLocation(context: Context) {
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat(Constants.GEOFENCE_LATITUDE, 0.0f).toDouble()
        val longitude = sharedPreferences.getFloat(Constants.GEOFENCE_LONGITUDE, 0.0f).toDouble()
        viewModelScope.launch(Dispatchers.IO) {
            durationAtLocation = activityDBViewModel.getTotalPresenceInLocation(latitude, longitude,startDate, endDate)
            locationInfo = activityDBViewModel.getAllLocationsInDate(startDate, endDate)
        }
    }

    fun getDurationAtLocationInHours(): Double{
        return if (durationAtLocation!=null) {
            durationAtLocation!! / 3600
        } else {
            0.0
        }
    }


}
