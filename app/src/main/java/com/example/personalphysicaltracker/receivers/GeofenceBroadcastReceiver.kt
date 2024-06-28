package com.example.personalphysicaltracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadCastReceiver(
    private val context: Context,
    private val intentAction: String,
    private val systemEvent: (userActivity: String) -> Unit
) : DefaultLifecycleObserver {


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = GeofenceStatusCodes
                        .getStatusCodeString(geofencingEvent.errorCode)
                    Log.e("GEOFENCE RECEIVER", errorMessage)
                    return
                }
                val geofenceTransition = geofencingEvent.geofenceTransition

                // Test that the reported transition was of interest.
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                    // Get the geofences that were triggered. A single event can trigger
                    // multiple geofences.
                    val triggeringGeofences = geofencingEvent.triggeringGeofences

                    // Get the transition details as a String.
                    val geofenceTransitionDetails = triggeringGeofences?.let {
                        getGeofenceTransitionDetails(
                            geofenceTransition,
                            it
                        )
                    }

                    // Send notification and log the transition details.
                    //sendNotification(geofenceTransitionDetails)
                    if (geofenceTransitionDetails != null) {
                        Log.i("GEOFENCE RECEIVER", geofenceTransitionDetails)
                    }
                } else {
                    // Log the error.
                    Log.e("GEOFENCE RECEIVER", "error")
                }
            }

            // Get the transition type.

        }
    }

    private fun getGeofenceTransitionDetails(
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val geofenceTransitionString = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered geofence(s): "
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited geofence(s): "
            else -> "Unknown transition: "
        }

        val geofenceIds = triggeringGeofences.map { it.requestId }
        return "$geofenceTransitionString ${geofenceIds.joinToString(", ")}"
    }
    override fun onCreate(owner: LifecycleOwner) {

    }



    override fun onDestroy(owner: LifecycleOwner) {

    }

    fun stopReceiver() {

    }



}

