package com.example.personalphysicaltracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.personalphysicaltracker.ui.settings.ActivityTransitionHandler
import com.example.personalphysicaltracker.ui.settings.NotificationService

class ActivityTransitionReceiver(
    private val context: Context,
    private val intentAction: String,
    private val systemEvent: (userActivity: String) -> Unit
) : DefaultLifecycleObserver {

    private var isOn = false;

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isOn = true
            val notificationService = NotificationService()
            val title = "Activity Changed!"

            val result = intent.let { ActivityTransitionResult.extractResult(it) } ?: return
            val printInformations = buildString {
                for (event in result.transitionEvents) {
                    append("${ActivityTransitionHandler.getActivityType(event.activityType)}: ")
                    append("${ActivityTransitionHandler.getTransitionType(event.transitionType)}\n\n")

                    ActivityTransitionHandler.handleEvent(event)
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
        Log.d("Activity Transition Receiver", "BroadcastReceiver registered")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (isOn) {
            context.unregisterReceiver(broadcastReceiver)
            isOn = false
        }
        Log.d("Activity Transition Receiver", "BroadcastReceiver unregistered")
    }

    fun stopReceiver() {
        if (isOn) {
            context.unregisterReceiver(broadcastReceiver)
            isOn = false
            Log.d("Activity Transition Receiver", "BroadcastReceiver unregistered")
        } else {
            Log.d("Activity Transition Receiver", "BroadcastReceiver not registered")
        }
    }





}

