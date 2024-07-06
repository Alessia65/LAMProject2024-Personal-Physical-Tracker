package com.example.personalphysicaltracker.services

import android.Manifest
import android.app.Notification
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
import com.example.personalphysicaltracker.MainActivity
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.utils.Constants

class NotificationServiceLocation : Service() {

    private val binder = SimpleBinderLoc()
    private var context: Context? = null

    inner class SimpleBinderLoc : Binder() {
        fun getService(): NotificationServiceLocation {
            return this@NotificationServiceLocation
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannelForLocationDetection()
        createPermanentNotificationLocationDetection(this)
        return START_STICKY
    }

    private fun createNotificationChannelForLocationDetection() {
        val channelId = Constants.CHANNEL_LOCATION_DETECTION_ID
        val nameLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_TITLE
        val descriptionLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_DESCRIPTION
        val importanceLocationDetectionChannel = NotificationManager.IMPORTANCE_HIGH
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            var channelLocationDetection = notificationManager.getNotificationChannel(channelId)
            if (channelLocationDetection == null) {
                channelLocationDetection = NotificationChannel(
                        channelId,
                        nameLocationDetectionChannel,
                        importanceLocationDetectionChannel
                    ).apply {
                        description = descriptionLocationDetectionChannel
                    }
                notificationManager.createNotificationChannel(channelLocationDetection)

            } else {
                Log.d("NotificationChannel", "Channel $channelId already exists")
            }
            } else {
            TODO("VERSION.SDK_INT < O")

        }



    }

    private fun createNotification(context: Context): Notification {
        this.context = context

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

    private fun createPermanentNotificationLocationDetection(context: Context) {
        this.context = context

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
        this.context = context

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_LOCATION_DETECTION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context,
            Constants.CHANNEL_LOCATION_DETECTION_ID
        )
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

    fun stopPermanentNotificationLocationDetection() {
        if (context != null) {
            deleteChannelLocationDetection()
            val notificationManager =
                context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(Constants.REQUEST_CODE_LOCATION_DETECTION)
            Log.d("NOTIFICATION SERVICE LOCATION", "CHANNEL DELETED")

        }
    }

    private fun deleteChannelLocationDetection() {
        if (context != null) {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Cancel the notification
            notificationManager.cancel(Constants.REQUEST_CODE_LOCATION_DETECTION)
            Log.d("NOT", "deleteChannelLocationDetection")
        } else {
            Log.e("NOT", "Application Context is null")
        }
    }

    override fun onDestroy() {
        stopPermanentNotificationLocationDetection()

        super.onDestroy()
        Log.d("LOC", "Service destroyed")


    }


}
