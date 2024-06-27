package com.example.personalphysicaltracker.ui.settings

import com.example.personalphysicaltracker.Constants
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleObserver
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await

class ActivityTransitionHandler(context: Context): LifecycleObserver {

    companion object {

        fun getActivityType(int: Int): String = when (int) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            else -> "UNKNOWN"
        }

        fun getTransitionType(int: Int): String = when (int) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "START"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "END"
            else -> "UNKNOWN"
        }
    }

    private val client = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION_CODE,
        Intent(Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION),
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) PendingIntent.FLAG_CANCEL_CURRENT else PendingIntent.FLAG_IMMUTABLE
    )

    private val activityTransitions = listOf(
        DetectedActivity.STILL,
        DetectedActivity.WALKING,
        DetectedActivity.IN_VEHICLE
    ).flatMap { activityType ->
        listOf(
            ActivityTransition.Builder().setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder().setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
        )
    }


    /*
    Nella guida utilizza onSuccess e onFailure ma cosi è piu veloce!! Senza await: 1mn, con: 30sec
     */


    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun registerActivityTransitions() = kotlin.runCatching {
        client.requestActivityTransitionUpdates(
            ActivityTransitionRequest(
                activityTransitions
            ), pendingIntent
        ).await()
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun unregisterActivityTransitions() = kotlin.runCatching {
        client.removeActivityUpdates(pendingIntent).await()
    }

    /*

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    fun registerActivityTransitions() {
        client.requestActivityTransitionUpdates(
            ActivityTransitionRequest(activityTransitions), pendingIntent
        ).addOnSuccessListener {
            Log.d("ACTIVITY TRANSITION REGISTER", "SUCCESS")
        }.addOnFailureListener { exception ->
            Log.d("ACTIVITY TRANSITION REGISTER", "UNSUCCESS")
        }
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    fun unregisterActivityTransitions() {
        client.removeActivityUpdates(pendingIntent)
            .addOnSuccessListener {
            Log.d("ACTIVITY TRANSITION UNREGISTER", "SUCCESS")
            }
            .addOnFailureListener { exception ->
            Log.d("ACTIVITY TRANSITION UNREGISTER", "UNSUCCESS")
            }
    }

     */
}

