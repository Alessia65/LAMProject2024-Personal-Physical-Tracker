package com.example.personalphysicaltracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.utils.Constants
import com.google.android.gms.location.ActivityTransitionResult

class NotificationServiceActivityRecognition : Service() {

    private val binder = SimpleBinderActivity()
    private lateinit var context: Context
    private lateinit var activityChangeReceiver: BroadcastReceiver
    var selectedActivity: PhysicalActivity? = null


    inner class SimpleBinderActivity : Binder() {
        fun getService(): NotificationServiceActivityRecognition {
            return this@NotificationServiceActivityRecognition
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        this.context = this
        registerActivityChangeReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "onStartCommand")
        createNotificationChannelForActivityRecognition()
        createPermanentNotificationActivityRecognition()

        return START_STICKY
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerActivityChangeReceiver() {
        activityChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                setIsOn(true)

                val title = "Activity Changed!"
                val result = ActivityTransitionResult.extractResult(intent)
                if (result == null) {
                    Log.e("ACTIVITY TRANSITION RECEIVER", "Failed to extract ActivityTransitionResult")
                    return
                }
                val printInfo = buildString {
                    for (event in result.transitionEvents) {
                        val activityType = ActivityTransitionHandler.getActivityType(event.activityType)
                        val transitionType = ActivityTransitionHandler.getTransitionType(event.transitionType)
                        append("$activityType:$transitionType\n")
                        ActivityTransitionHandler.handleEvent(activityType, transitionType)
                    }
                }

                showActivityChangesNotification(context, title, printInfo)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("ACTIVITY TRANSITION RECEIVER", "Using registerReceiver with Context.RECEIVER_NOT_EXPORTED")
            registerReceiver(activityChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            Log.d("ACTIVITY TRANSITION RECEIVER", "Using registerReceiver without flags")
            registerReceiver(activityChangeReceiver, filter)
        }
        setIsOn(true)
    }

    private fun unregisterActivityChangeReceiver() {
        try {
            if (checkIsOn()) {
                unregisterReceiver(activityChangeReceiver)
                setIsOn(false)
                Log.d("ACTIVITY TRANSITION RECEIVER", "BroadcastReceiver unregistered")
            } else {
                Log.d("ACTIVITY TRANSITION RECEIVER", "BroadcastReceiver not registered")

            }
        }catch (e: Exception){
            Log.e("ACTIVITY RECEIVER", "Error")
            setIsOn(false)

        }
    }

    private fun createNotification(context: Context): Notification {
        this.context = context

        return NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
            .setContentTitle("Activity Recognition Service On")
            .setContentText("Don't close the app.")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(null)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createPermanentNotificationActivityRecognition() {
        this.context.let { context ->
            val notification = createNotification(context)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification)
            } else {
                Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "Permission not Granted")
            }
        }
    }

    fun showActivityChangesNotification(context: Context, title: String, message: String) {
        this.context = context

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ACTIVITY_RECOGNITION_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(null)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION, notification)
            } else {
                Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "Permission not Granted")
            }
        }
    }

    private fun createNotificationChannelForActivityRecognition() {
        val channelId = Constants.CHANNEL_ACTIVITY_RECOGNITION_ID
        val name = Constants.CHANNEL_ACTIVITY_RECOGNITION_TITLE
        val description = Constants.CHANNEL_ACTIVITY_RECOGNITION_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        var channel = notificationManager.getNotificationChannel(channelId)
        if (channel == null) {
            channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
            }
            notificationManager.createNotificationChannel(channel)
        } else {
            Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "Channel $channelId already exists")
        }
    }

    private fun stopPermanentNotificationActivityRecognition() {
        deleteChannelActivityRecognition()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION)
        Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "CHANNEL DELETED")
    }

    override fun onDestroy() {
        Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "onDestroy")

        unregisterActivityChangeReceiver()
        stopPermanentNotificationActivityRecognition()
        stopForegroundService()
        super.onDestroy()
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(this.context, NotificationServiceActivityRecognition::class.java)
        this.context.stopService(serviceIntent)
    }

    fun stopAll(){
        Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "Stopped")
        unregisterActivityChangeReceiver()
        stopPermanentNotificationActivityRecognition()
        stopForegroundService()

    }


    private fun deleteChannelActivityRecognition() {
        if (context != null) {
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(Constants.REQUEST_CODE_ACTIVITY_RECOGNITION)
            Log.d("NOTIFICATION SERVICE ACTIVITY RECOGNITION", "deleteChannelActivityRecognition")
        }
    }

    private fun checkIsOn(): Boolean {
        val sharedPreferencesBackgroundActivities = getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )
        return sharedPreferencesBackgroundActivities.getBoolean(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false
        )
    }

    fun setIsOn(value: Boolean) {
        val sharedPreferencesBackgroundActivities = getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, value)
        editor.apply()
    }
}
