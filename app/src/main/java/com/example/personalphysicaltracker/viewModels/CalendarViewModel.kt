package com.example.personalphysicaltracker.viewModels

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.handlers.ShareHandler
import com.example.personalphysicaltracker.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class CalendarViewModel : ViewModel() {

    private var endDate: String = ""
    private var startDate: String = ""
    private var durationAtLocation: Double = 0.0
    // ViewModel to interact with activity data
    private lateinit var activityViewModel: ActivityViewModel

    // List to hold activities fetched from the database
    var activitiesOnDb: List<PhysicalActivity> = emptyList()

    // List to hold activities to be sent or displayed in another fragment
    var activitiesToSend: List<PhysicalActivity> = emptyList()
    private val selectedFilters: MutableList<String> = mutableListOf()


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
    private fun updateFromDatabase(): List<PhysicalActivity> {
        viewModelScope.launch(Dispatchers.IO) {
            activitiesOnDb = activityViewModel.getListOfPhysicalActivities()
            for (a in activitiesOnDb){
                Log.d("IN RANGE", a.getActivityTypeName().toString() + ", " + a.start + ", " + a.end + ", " + a.date + ", " + a.duration)
            }
        }
        return activitiesOnDb
    }

    /**
     * Retrieves the list of dates from the ViewModel.
     * These dates represent activities fetched from the database.
     */
    fun obtainDates(): List<PhysicalActivity> {
        return activitiesOnDb
    }



    /**
     * Retrieves the list of activities saved for transaction (to be sent or displayed in another fragment).
     */
    fun obtainActivitiesForTransaction(): List<PhysicalActivity> {
        updateFromDatabase()
        return activitiesToSend
    }

     fun handleSelectedDateRange(startDate: String, endDate: String) {
         this.startDate = startDate
         this.endDate = endDate
         activitiesToSend = obtainDates()
        Log.d("ACTIVITIES FROM DB", activitiesToSend.size.toString())

         Log.d("STARTDATE", startDate)
         Log.d("ENDDATE", endDate)
        // Filter activities based on selected date range
         activitiesToSend =  activitiesToSend.filter {
            it.date >= startDate && it.date <= endDate

        }

         // Applica tutti i filtri selezionati
         activitiesToSend = applyFilters(activitiesToSend)
         Log.d("ACTIVITIES FILTERED", activitiesToSend.size.toString())

     }



    fun addFilter(activityType: String): List<PhysicalActivity> {
        // Aggiungi il filtro solo se non è già presente
        if (!selectedFilters.contains(activityType)) {
            selectedFilters.add(activityType)
        }
        return applyFilters(activitiesToSend)
    }

    fun removeFilter(activityType: String): List<PhysicalActivity> {
        // Rimuovi il filtro se è presente
        selectedFilters.remove(activityType)
        return applyFilters(activitiesToSend)
    }

    private fun applyFilters(activities: List<PhysicalActivity>): List<PhysicalActivity> {
        // Se non ci sono filtri selezionati, restituisci tutte le attività
        if (selectedFilters.isEmpty()) {
            return activities
        }

        // Filtra le attività in base ai filtri selezionati
        return activities.filter { activity ->
            selectedFilters.any { filter ->
                activity.getActivityTypeName().toString() == filter
            }
        }
    }

    fun clearFilters() {
        selectedFilters.clear()
    }

    fun exportActivitiesToCSV(context: Context, activities: List<PhysicalActivity>) {
        ShareHandler.exportActivitiesToCSV(context, activities)
    }

    fun getTotalPresenceInLocation(context: Context) {
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat(Constants.GEOFENCE_LATITUDE, 0.0f).toDouble()
        val longitude = sharedPreferences.getFloat(Constants.GEOFENCE_LONGITUDE, 0.0f).toDouble()
        viewModelScope.launch {
            durationAtLocation =
                activityViewModel.getTotalPresenceInLocation(latitude, longitude,startDate, endDate)!!
            }
    }

    fun getDurationAtLocationInHours(): Double{
        return durationAtLocation/3600
    }


}
