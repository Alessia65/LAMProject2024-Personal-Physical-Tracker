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
import com.example.personalphysicaltracker.utils.NotificationService
import com.example.personalphysicaltracker.utils.NotificationServiceActivityRecognition

class ActivityTransitionReceiver(
    private val context: Context,
    private val intentAction: String,
    private val systemEvent: (userActivity: String) -> Unit
) : DefaultLifecycleObserver {

    private var isOn = false;
    private lateinit var notificationService : NotificationServiceActivityRecognition

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isOn = true

            val title = "Activity Recognition On: Activity Changed!"

            val result = intent.let { ActivityTransitionResult.extractResult(it) } ?: return
            val printInformations = buildString {
                for (event in result.transitionEvents) {
                    val activityType = (ActivityTransitionHandler.getActivityType(event.activityType))
                    val transitionType = ActivityTransitionHandler.getTransitionType(event.transitionType)
                    append("$activityType:$transitionType\n")

                    ActivityTransitionHandler.handleEvent(activityType, transitionType)
                }
            }

            val message = printInformations
            notificationService.showActivityChangesNotification(context, title, message)
            Log.d("Activity Transition Receiver", "onReceive: $printInformations")
            systemEvent(printInformations)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("ACTIVITY TRANSITION RECEIVER", "RECEIVER_NOT_EXPORTED")
                context.registerReceiver(broadcastReceiver, IntentFilter(intentAction), RECEIVER_NOT_EXPORTED)
        } else {
            Log.d("ACTIVITY TRANSITION RECEIVER", "NOT NECESSARY RECEIVER_NOT_EXPORTED")
            context.registerReceiver(broadcastReceiver, IntentFilter(intentAction))
        }
        isOn = true
        startActivityTransitionService()
        notificationService = NotificationServiceActivityRecognition()
        //notificationService.createPermanentNotificationActivityRecognition(context)
        Log.d("Activity Transition Receiver", "BroadcastReceiver registered")
    }



    /*
    override fun onDestroy(owner: LifecycleOwner) {
        if (isOn) {
            //context.unregisterReceiver(broadcastReceiver)
            //isOn = false
        }
        //stopActivityTransitionService()
        //Log.d("Activity Transition Receiver", "BroadcastReceiver unregistered")
    }

     */

    fun stopReceiver() {
        if (isOn) {
            context.unregisterReceiver(broadcastReceiver)
            isOn = false
            stopActivityTransitionService()
            Log.d("Activity Transition Receiver", "BroadcastReceiver unregistered")
        } else {
            Log.d("Activity Transition Receiver", "BroadcastReceiver not registered")
        }
    }

    private fun startActivityTransitionService() {
        val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
        ContextCompat.startForegroundService(context,intent)
        Log.d("ACT TRAN", "foregound on")

    }


    private fun stopActivityTransitionService() {
        val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
        context.stopService(intent)
        notificationService.stopPermanentNotificationActivityRecognition()

    }



}

