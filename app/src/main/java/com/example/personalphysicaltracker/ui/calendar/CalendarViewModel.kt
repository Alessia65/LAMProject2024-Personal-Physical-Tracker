package com.example.personalphysicaltracker.ui.calendar

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarViewModel : ViewModel() {

    // ViewModel to interact with activity data
    private lateinit var activityViewModel: ActivityViewModel

    // List to hold activities fetched from the database
    var activitiesOnDb: List<PhysicalActivity> = emptyList()

    // List to hold activities to be sent or displayed in another fragment
    var activitiesToSend: List<PhysicalActivity> = emptyList()

    /**
     * Initializes the ActivityViewModel using the provided FragmentActivity.
     * This allows sharing the ViewModel between fragments.
     */
    fun initializeActivityViewModel(activity: FragmentActivity?) {
        // Ensure activity is not null
        val viewModelStoreOwner = activity ?: throw IllegalArgumentException("Activity must not be null")

        // Create repository and ViewModelFactory instances
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)

        // Initialize the ActivityViewModel
        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityViewModel::class.java]

        // Load daily data from the database when ViewModel is initialized
        updateFromDatabase()
    }

    /**
     * Fetches the latest data from the database using the ActivityViewModel.
     * Runs in the IO dispatcher to perform database operations.
     */
    private fun updateFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            activitiesOnDb = activityViewModel.getListOfPhysicalActivities()

            // Uncomment to log activities fetched from the database
            /*
            for (activity in activitiesOnDb) {
                Log.d("ACTIVITIES", "${activity.getActivityTypeName()}, ${activity.date}, ${activity.start}, ${activity.end}, ${activity.duration}")
            }
            */
        }
    }

    /**
     * Retrieves the list of dates from the ViewModel.
     * These dates represent activities fetched from the database.
     */
    fun obtainDates(): List<PhysicalActivity> {
        return activitiesOnDb
    }

    /**
     * Saves the list of activities to be sent or displayed in another fragment.
     */
    fun saveActivitiesForTransaction(activities: List<PhysicalActivity>) {
        activitiesToSend = activities
    }

    /**
     * Retrieves the list of activities saved for transaction (to be sent or displayed in another fragment).
     */
    fun obtainActivitiesForTransaction(): List<PhysicalActivity> {
        return activitiesToSend
    }
}
