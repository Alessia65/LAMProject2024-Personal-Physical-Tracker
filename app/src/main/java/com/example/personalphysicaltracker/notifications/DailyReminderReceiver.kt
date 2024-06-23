package com.example.personalphysicaltracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.personalphysicaltracker.notifications.NotificationService

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Verifica che il contesto non sia nullo prima di procedere
        if (context == null) return

        // Utilizza applicationContext per garantire un contesto valido
        val notificationService = NotificationService()
        val title = "Daily Reminder"
        val message = "Remember to track your physical activities today!"

        notificationService.showDailyReminderNotification(context.applicationContext, title, message)
    }
}
