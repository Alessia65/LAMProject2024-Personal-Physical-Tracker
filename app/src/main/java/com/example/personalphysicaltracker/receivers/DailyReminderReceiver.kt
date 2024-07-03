package com.example.personalphysicaltracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.personalphysicaltracker.services.NotificationService

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("DAILY REMINDER RECEIVER", "ON")
        // Verifica che il contesto non sia nullo prima di procedere
        if (context == null) return

        // Utilizza applicationContext per garantire un contesto valido
        val notificationService = NotificationService()
        val title = "Daily Reminder"
        val message = "Remember to track your physical activities today!"

        Log.d("NOTIFICATION", "SENDING DAILY REMINDER")
        notificationService.showDailyReminderNotification(context.applicationContext, title, message)
    }
}
