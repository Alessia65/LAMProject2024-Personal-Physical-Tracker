package com.example.personalphysicaltracker.ui.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.example.personalphysicaltracker.database.*
import com.example.personalphysicaltracker.notifications.DailyReminderReceiver
import com.example.personalphysicaltracker.notifications.StepsReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


     fun scheduleStepsNotification(selectedNumber: Int, calendar: Calendar, dailySteps: Long, context: Context) {

        // Verifica se la somma dei passi giornalieri è inferiore all'obiettivo selezionato
        val dailyStepsSum = dailySteps // Implementa questa funzione per calcolare la somma dei passi giornalieri

        if (dailyStepsSum < selectedNumber) {
            Log.d("StepsReminder", "Daily steps goal: $selectedNumber, current: $dailyStepsSum")
            // Invia la notifica degli steps solo se la somma è inferiore all'obiettivo
            createStepsNotification(calendar, context)
        } else {
            // Non fare nulla, la somma dei passi è sufficiente
            Log.d("StepsReminder", "Daily steps goal already met.")
        }

        // Salva l'obiettivo selezionato nelle SharedPreferences
        val sharedPreferences = context.getSharedPreferences("settings minimum steps reminder notification", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("steps_reminder", selectedNumber)
        editor.apply()
    }

    private fun createStepsNotification(calendar: Calendar, context: Context) {

        // Create an intent for StepsReminderReceiver
        val intent = Intent(context, StepsReminderReceiver::class.java)
        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1, // Codice per steps
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
        

        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, DailyReminderReceiver::class.java)
        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, //0 per daily reminder
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
        val sharedPreferences = context.getSharedPreferences("settings daily reminder notification", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("daily_reminder_hour", hour)
        editor.putInt("daily_reminder_minute", minute)
        editor.putString("daily_reminder_formatted_time", formattedTime)
        editor.apply()
    }
    fun cancelStepsNotification(context: Context){
        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, StepsReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1, //1 per steps
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

     fun cancelDailyNotification(context: Context) {
        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, DailyReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, //0 per daily reminder
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

}