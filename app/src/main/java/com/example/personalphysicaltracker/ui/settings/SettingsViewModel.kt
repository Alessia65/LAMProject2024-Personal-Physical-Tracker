package com.example.personalphysicaltracker.ui.settings

import android.app.Activity
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

// Communication ActivityViewModel and HomeFragment
class SettingsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel


    // Initialize the ViewModel with the necessary repository for database operations
    fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityViewModel::class.java]
    }

    fun getTotalStepsFromToday(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, callback: (Long) -> Unit) {
        initializeActivityViewModel(activity, viewModelStoreOwner)
        val today = getCurrentDay()
        viewModelScope.launch {
            var totalSteps = 0L
            try {
                // Execute the query in the IO thread to get the duration
                totalSteps = withContext(Dispatchers.IO) {
                    activityViewModel.getTotalStepsFromToday(today)
                }
            } catch (e: Exception) {
                // Exception handling, such as logging or other types of handling
                Log.e("SettingsViewModel", "Exception while fetching duration", e)
            }
            callback(totalSteps)
        }
    }



    // Get the current date in the specified format
    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

}