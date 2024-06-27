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

// Communication ActivityViewModel and HomeFragment
class HomeViewModel : ViewModel() {


    // Initialize the ViewModel with the necessary repository for database operations
    fun initializeActivityHandler(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {
        viewModelScope.launch {
            ActivityHandler.initialize(activity, viewModelStoreOwner)
        }
    }


    // Start a selected activity
    fun startSelectedActivity(activity: PhysicalActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            ActivityHandler.startSelectedActivity(activity)
        }
    }

    // Get the current date in the specified format

    // Stop the selected activity, calculate the duration, and update the daily values
    fun stopSelectedActivity(steps: Long, isWalkingActivity: Boolean) {
        viewModelScope.launch (Dispatchers.IO) {
            setSteps(steps)
            ActivityHandler.stopSelectedActivity(isWalkingActivity)
        }

    }

    fun setSteps(totalSteps: Long) {
        ActivityHandler.setSteps(totalSteps)
    }


}