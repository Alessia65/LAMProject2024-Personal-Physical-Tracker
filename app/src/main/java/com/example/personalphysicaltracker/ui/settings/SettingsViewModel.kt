package com.example.personalphysicaltracker.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.receivers.DailyReminderReceiver
import com.example.personalphysicaltracker.receivers.StepsReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Communication ActivityViewModel and HomeFragment
class SettingsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel
    private var stepsWanted: Long = 0
    private var dailySteps: Long = 0


    // Initialize the ViewModel with the necessary repository for database operations
    fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityViewModel::class.java]
    }

    fun getStepsWanted(): Long{
        return stepsWanted
    }

    fun getDailySteps():Long{
        return dailySteps
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


     fun scheduleStepsNotification(selectedNumber: Int, calendar: Calendar, dailySteps: Long, context: Context) {

         // Invia la notifica degli steps solo se la somma è inferiore all'obiettivo
         createStepsNotification(calendar, context)


        // Salva l'obiettivo selezionato nelle SharedPreferences
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, selectedNumber)
        editor.apply()
    }

    private fun createStepsNotification(calendar: Calendar, context: Context) {

        val date: Date = calendar.time

        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val formattedDate: String = sdf.format(date)

        Log.d("NOTIFICATION","CREATE STEP REMINDER RECEIVER at" + formattedDate)
        // Create an intent for StepsReminderReceiver
        val intent = Intent(context, StepsReminderReceiver::class.java)
        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_STEPS_REMINDER, // Codice per steps
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to schedule the notification
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        // Inform the user that the steps notification will be set for a specific time
    }

    fun scheduleDailyNotification(
        calendar: Calendar,
        context: Context,
        hour: Int,
        minute: Int,
        formattedTime: String
    ) {
        Log.d("NOTIFICATION","CREATE DAILY REMINDER RECEIVER at" + formattedTime)

        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, DailyReminderReceiver::class.java)

        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_DAILY_REMINDER, //0 per daily reminder
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to schedule the notification
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )


        // Save the formatted notification time in SharedPreferences
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, hour)
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, minute)
        editor.putString(Constants.SHARED_PREFERENCES_DAILY_REMINDER_FORMATTED_TIME, formattedTime)
        editor.apply()
    }
    fun cancelStepsNotification(context: Context){
        Log.d("NOTIFICATION","DELETE STEP REMINDER RECEIVER")

        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, StepsReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_STEPS_REMINDER, //1 per steps
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

     fun cancelDailyNotification(context: Context) {
         Log.d("NOTIFICATION","DELETE DAILY REMINDER RECEIVER")

         // Create an intent for DailyReminderReceiver
        val intent = Intent(context, DailyReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_DAILY_REMINDER, //0 per daily reminder
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
    // Get the current date in the specified format
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


}