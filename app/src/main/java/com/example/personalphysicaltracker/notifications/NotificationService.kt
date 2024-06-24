package com.example.personalphysicaltracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.MainActivity
import com.example.personalphysicaltracker.R
import java.util.Date

class NotificationService : Service() {

    inner class SimpleBinder : Binder() {
        fun getService(): NotificationService {
            return this@NotificationService
        }
    }

    private val binder = SimpleBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
    }


    fun showDailyReminderNotification(context: Context, title: String, message: String) {
        Log.d("SHOW DAILY NOTIFICATION","FROM NOTIFICATION SERVICE")

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_DAILY_REMINDER, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_DAILY_REMINDER_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Chiude la notifica quando viene cliccata
            .build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(Constants.REQUEST_CODE_DAILY_REMINDER, notification)
        }
    }

    fun showStepsReminderNotification(context: Context, title: String, message: String) {
        Log.d("SHOW STEP NOTIFICATION","FROM NOTIFICATION SERVICE")

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, Constants.REQUEST_CODE_STEPS_REMINDER, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_STEPS_REMINDER_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Chiude la notifica quando viene cliccata
            .build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(Constants.REQUEST_CODE_STEPS_REMINDER, notification)
        }
    }
}
