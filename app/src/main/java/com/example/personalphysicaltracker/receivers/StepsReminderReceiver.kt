package com.example.personalphysicaltracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.services.NotificationService

class StepsReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Verifica che il contesto non sia nullo prima di procedere
        if (context == null) return

        // Utilizza applicationContext per garantire un contesto valido
        val notificationService = NotificationService()
        val title = "Come on!"
        val message = "You haven't completed your daily goal!"

        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val stepsNumber = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, 0)
        val dailySteps = sharedPreferences.getLong(Constants.SHARED_PREFERENCES_STEPS_DAILY, 0)
        if (dailySteps < stepsNumber) {
            Log.d("STEP REMINDER RECEIVER", "SENDING, dailysteps: $dailySteps, stepsNumber: $stepsNumber")
            notificationService.showStepsReminderNotification(context.applicationContext, title, message)

        } else {
            Log.d("STEP REMINDER RECEIVER", "NOT SENDING, dailysteps: $dailySteps, stepsNumber: $stepsNumber")

        }

    }
}
