package com.example.personalphysicaltracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityTransitionResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.services.NotificationServiceActivityRecognition
import com.example.personalphysicaltracker.utils.Constants

class ActivityTransitionReceiver(private val context: Context, private val intentAction: String, private val systemEvent: (userActivity: String) -> Unit) : DefaultLifecycleObserver {

    private lateinit var notificationService : NotificationServiceActivityRecognition // Notification service instance

    // Inner class to handle broadcast events
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setIsOn(true)

            val title = "Activity Recognition On: Activity Changed!"

            // Extracting activity transition result from the intent
            val result = ActivityTransitionResult.extractResult(intent)
            if (result == null) {
                Log.e("ACTIVITY TRANSITION RECEIVER", "Failed to extract ActivityTransitionResult")
                return
            }

            // Building a string with activity transition information
            val printInformations = buildString {
                for (event in result.transitionEvents) {
                    val activityType = (ActivityTransitionHandler.getActivityType(event.activityType))
                    val transitionType = ActivityTransitionHandler.getTransitionType(event.transitionType)
                    append("$activityType:$transitionType\n")

                    ActivityTransitionHandler.handleEvent(activityType, transitionType)
                }
            }

            val message = printInformations

            // Showing a notification with activity transition information
            notificationService.showActivityChangesNotification(context, title, message)
            Log.d("ACTIVITY TRANSITION RECEIVER", "onReceive: $printInformations")

            // Triggering the system event callback with the activity information
            systemEvent(printInformations)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        notificationService = NotificationServiceActivityRecognition()
        val intentFilter = IntentFilter(intentAction)

        // Registering the broadcast receiver with appropriate flags based on the Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("ACTIVITY TRANSITION RECEIVER", "RECEIVER_NOT_EXPORTED")
                context.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            Log.d("ACTIVITY TRANSITION RECEIVER", "NOT NECESSARY RECEIVER_NOT_EXPORTED")
            context.registerReceiver(broadcastReceiver, intentFilter)
        }
        setIsOn(true)
        startActivityTransitionService()
        Log.d("ACTIVITY TRANSITION RECEIVER", "BroadcastReceiver registered")
    }



    fun stopReceiver() {
        if (checkIsOn()) {
            context.unregisterReceiver(broadcastReceiver)
            setIsOn(false)
            stopActivityTransitionService()
            Log.d("ACTIVITY TRANSITION RECEIVER", "BroadcastReceiver unregistered")
        } else {
            Log.d("ACTIVITY TRANSITION RECEIVER", "BroadcastReceiver not registered")
        }
    }

    private fun startActivityTransitionService() {
        val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
        ContextCompat.startForegroundService(context,intent)
        Log.d("ACTIVITY TRANSITION RECEIVER", "startActivityTransitionService")

    }


    private fun stopActivityTransitionService() {
        val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
        context.stopService(intent)
        notificationService.stopPermanentNotificationActivityRecognition()

    }

    private fun checkIsOn(): Boolean{
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )

        return sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false)

    }

    fun setIsOn(value: Boolean){
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, value)
        editor.apply()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopReceiver()
        super.onDestroy(owner)
    }

}

