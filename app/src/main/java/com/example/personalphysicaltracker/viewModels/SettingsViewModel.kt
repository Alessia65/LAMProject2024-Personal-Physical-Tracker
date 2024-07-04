package com.example.personalphysicaltracker.viewModels

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.handlers.NotificationHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Communication ActivityViewModel and HomeFragment
class SettingsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityDBViewModel


    // Initialize the ViewModel with the necessary repository for database operations
    private fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
    }

    fun getTotalStepsFromToday(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, callback: (Long) -> Unit) {
        initializeActivityViewModel(activity, viewModelStoreOwner)
        val today = getCurrentDay()
        viewModelScope.launch {
            var totalSteps = 0L
            try {
                totalSteps = withContext(Dispatchers.IO) {

                    activityViewModel.getTotalStepsFromToday(today)
                }

            } catch (e: Exception) {
                // Exception handling, such as logging or other types of handling
                Log.e("SettingsViewModel", "Exception with total steps = 0")
                totalSteps = 0
            }
            callback(totalSteps)
        }
    }


     fun scheduleStepsNotification(selectedNumber: Int, context: Context) {

         // Salva l'obiettivo selezionato nelle SharedPreferences
         val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
         val editor = sharedPreferences.edit()
         editor.putInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, selectedNumber)
         editor.apply()

         // Invia la notifica degli steps solo se la somma è inferiore all'obiettivo
         NotificationHandler.scheduleStepsNotificationIfEnabled(context)



    }


    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int, formattedTime: String) {

        //Set sharedPreferences:
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, hour)
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, minute)
        editor.putString(Constants.SHARED_PREFERENCES_DAILY_REMINDER_FORMATTED_TIME, formattedTime)
        editor.apply()

        NotificationHandler.scheduleDailyNotificationIfEnabled(context)

    }
    fun cancelStepsNotification(context: Context){
        NotificationHandler.cancelStepsNotification(context)
    }

     fun cancelDailyNotification(context: Context) {
         NotificationHandler.cancelDailyNotification(context)
     }

    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun checkBackgroundRecogniseActivitiesOn(context: Context): Boolean{
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        return  (sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false))
    }

    fun setBackgroundRecogniseActivies(context: Context, isChecked: Boolean){
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, isChecked)
        editor.apply()
    }

    fun checkBackgroundLocationDetection(context: Context): Boolean {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        return  (sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false))
    }

    fun setBackgroundLocationDetection(context: Context, isChecked: Boolean) {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, isChecked)
        editor.apply()
        Log.d("SETTO ENABLED", isChecked.toString())
    }

    fun getActivityViewModel(): ActivityDBViewModel{
        return activityViewModel
    }

}