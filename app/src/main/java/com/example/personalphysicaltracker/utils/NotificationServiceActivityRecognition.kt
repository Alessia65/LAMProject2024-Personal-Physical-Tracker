package com.example.personalphysicaltracker.utils

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

class NotificationServiceActivityRecognition : Service() {

    private val binder = SimpleBinderActivity()
    private var context: Context? = null
    inner class SimpleBinderActivity : Binder() {
        fun getService(): NotificationServiceActivityRecognition {
            return this@NotificationServiceActivityRecognition
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NsAR", "On start")
        createNotificationChannelForActivityRecognition()
        createPermanentNotificationActivityRecognition(this)

        return START_STICKY
    }

    private fun createNotification(context: Context): Notification {
        this.context = context
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_ACTIVITY_RECOGNITION,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
            .setContentTitle("Activity Recognition Service On")
            .setContentText("Don't close the app.")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun createPermanentNotificationActivityRecognition(context: Context) {
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

        startForeground(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification)
        Log.d("NsAR", "created")

    }

    fun showActivityChangesNotification(context: Context, title: String, message: String) {
        this.context = context

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_ACTIVITY_RECOGNITION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
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
            notify(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification) // Using a different ID for activity change notifications
        }
    }

    fun createNotificationChannelForActivityRecognition() {

        val channelId = Constants.CHANNEL_ACTIVITY_RECOGNITION_ID
        val name = Constants.CHANNEL_ACTIVITY_RECOGNITION_TITLE
        val description = Constants.CHANNEL_ACTIVITY_RECOGNITION_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verifica se il canale delle notifiche esiste già
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                // Il canale non esiste, quindi lo creiamo
                channel = NotificationChannel(channelId, name, importance).apply {
                    this.description = description
                }
                notificationManager.createNotificationChannel(channel)
            } else {
                // Il canale esiste già, non fare nulla
                Log.d("NotificationChannel", "Channel $channelId already exists")
            }
        } else {
            // Gestione per versioni precedenti a Oreo
            // In questo caso, il canale delle notifiche viene creato comunque ogni volta
            TODO("VERSION.SDK_INT < O")
        }
    }


    fun stopPermanentNotificationActivityRecognition() {
        Log.d("NSAR", "SONO QUI")
        if (context!= null) {
            deleteChannelActivityRecognition()
            val notificationManager =
                context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION)
            Log.d("CHANNEL", "CHANNEL DELETED")
        }
    }



    override fun onDestroy() {
        stopPermanentNotificationActivityRecognition()
        super.onDestroy()
        Log.d("NsAR", "Service destroyed")
    }

    private fun deleteChannelActivityRecognition() {
        if (context != null) {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Cancel the notification
            notificationManager.cancel(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION)
            Log.d("NOT", "deleteChannelActivityRecognition ")
        }
    }
}
