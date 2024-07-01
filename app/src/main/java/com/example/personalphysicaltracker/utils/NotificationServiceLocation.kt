package com.example.personalphysicaltracker.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.personalphysicaltracker.MainActivity
import com.example.personalphysicaltracker.R

class NotificationServiceLocation : Service() {

    private val binder = SimpleBinderLoc()

    inner class SimpleBinderLoc : Binder() {
        fun getService(): NotificationServiceLocation {
            return this@NotificationServiceLocation
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createPermanentNotificationLocationDetection(this)
        return START_STICKY
    }

    private fun createNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_LOCATION_DETECTION,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.CHANNEL_LOCATION_DETECTION_ID)
            .setContentTitle("Location Detection On")
            .setContentText("Don't close the app.")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun createPermanentNotificationLocationDetection(context: Context) {
        val notification = createNotification(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NOTIFICATION SERVICE", "Permission not Granted")
            return
        }

        startForeground(Constants.REQUEST_CODE_LOCATION_DETECTION, notification)
    }

    fun showLocationChangesNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_LOCATION_DETECTION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_LOCATION_DETECTION_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NOTIFICATION SERVICE", "Permission not Granted")
                return
            }
            notify(Constants.REQUEST_CODE_LOCATION_DETECTION, notification)
        }
    }

    fun stopPermanentNotificationLocationDetection(context: Context) {
        stopForeground(true)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.REQUEST_CODE_LOCATION_DETECTION)
    }
}
