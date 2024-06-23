package com.example.personalphysicaltracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StepsReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Verifica che il contesto non sia nullo prima di procedere
        if (context == null) return

        // Utilizza applicationContext per garantire un contesto valido
        val notificationService = NotificationService()
        val title = "Come on!"
        val message = "You haven't completed your daily goal!"

        notificationService.showStepsReminderNotification(context.applicationContext, title, message)
    }
}
