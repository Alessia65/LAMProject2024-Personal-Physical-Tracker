package com.example.personalphysicaltracker.ui.settings

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

class ActivityTransitionReceiver(
    private val context: Context,
    private val intentAction: String,
    private val systemEvent: (userActivity: String) -> Unit
) : DefaultLifecycleObserver {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val result = intent?.let { ActivityTransitionResult.extractResult(it) } ?: return
            val printInformations = buildString {
                for (event in result.transitionEvents) {
                    append("${UserActivityTransitionManager.getActivityType(event.activityType)}: ")
                    append("${UserActivityTransitionManager.getTransitionType(event.transitionType)}\n\n")
                }
            }
            Log.d("Activity Transition Receiver", "onReceive: $printInformations")
            systemEvent(printInformations)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("ACTIVITY TRANSITION RECEIVER", "RECEIVER_NOT_EXPORTED")
                context.registerReceiver(broadcastReceiver, IntentFilter(intentAction), RECEIVER_NOT_EXPORTED)
        } else {
            Log.d("ACTIVITY TRANSITION RECEIVER", "NOT NECESSARY RECEIVER_NOT_EXPORTED")
            context.registerReceiver(broadcastReceiver, IntentFilter(intentAction))

        }
        Log.d("Activity Transition Receiver", "BroadcastReceiver registered")
    }

    override fun onStop(owner: LifecycleOwner) {
        context.unregisterReceiver(broadcastReceiver)
        Log.d("Activity Transition Receiver", "BroadcastReceiver unregistered")
    }
}

