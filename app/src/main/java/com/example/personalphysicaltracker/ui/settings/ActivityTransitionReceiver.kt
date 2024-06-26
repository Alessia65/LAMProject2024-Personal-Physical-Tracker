package com.example.personalphysicaltracker.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        override fun onReceive(context: Context?, intent: Intent?) {
            val result = intent?.let { ActivityTransitionResult.extractResult(it) } ?: return
            val printInformations = buildString {
                for (event in result.transitionEvents) {
                    append("${UserActivityTransitionManager.getActivityType(event.activityType)} - ")
                    append("${UserActivityTransitionManager.getTransitionType(event.transitionType)}\n\n")
                }
            }
            Log.d("ActivityTransitionReceiver", "onReceive: $printInformations")
            systemEvent(printInformations)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        context.registerReceiver(broadcastReceiver, IntentFilter(intentAction))
        Log.d("ActivityTransitionReceiver", "BroadcastReceiver registered")
    }

    override fun onStop(owner: LifecycleOwner) {
        context.unregisterReceiver(broadcastReceiver)
        Log.d("ActivityTransitionReceiver", "BroadcastReceiver unregistered")
    }
}

