package com.example.personalphysicaltracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.personalphysicaltracker.services.NotificationService

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val notificationService = NotificationService()
        val title = "Daily Reminder"
        val message = "Remember to track your physical activities today!"

        Log.d("NOTIFICATION", "SENDING DAILY REMINDER")
        notificationService.showDailyReminderNotification(context.applicationContext, title, message)
    }
}
