package com.example.personalphysicaltracker.ui.home

import android.content.Context
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
    fun initializeActivityHandler(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, context: Context) {
        viewModelScope.launch {
            ActivityHandler.initialize(activity, viewModelStoreOwner, context)
        }
    }


    fun startSelectedActivity(activity: PhysicalActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            ActivityHandler.startSelectedActivity(activity)
        }
    }


    fun setSteps(totalSteps: Long) {
        ActivityHandler.setSteps(totalSteps)
    }


}