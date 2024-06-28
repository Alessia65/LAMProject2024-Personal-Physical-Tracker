package com.example.personalphysicaltracker.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.personalphysicaltracker.Constants
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

@SuppressLint("StaticFieldLeak")
object GeofenceHandler {


    lateinit  var geofencingClient: GeofencingClient
    lateinit var context: Context
    var geofenceList = mutableMapOf<String, Geofence>()
    lateinit var pendingIntent: PendingIntent


    fun initialize(context: Context){
        this.context = context
        geofencingClient = LocationServices.getGeofencingClient(context)
        pendingIntent =
            PendingIntent.getBroadcast(
                context,
                Constants.BACKGROUND_OPERATION_LOCATION_DETECTION_CODE,
                Intent(Constants.BACKGROUND_OPERATION_LOCATION_DETECTION),
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) PendingIntent.FLAG_CANCEL_CURRENT else PendingIntent.FLAG_IMMUTABLE
            )
    }

    fun addGeofence(key: String, location: Location){

        geofenceList[key] = Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(location.latitude, location.longitude, Constants.GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
            .build()

    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) //Se per un periodo di tempo: INITIAL_TRIGGER_DWELL
            addGeofences(geofenceList.values.toList())
        }.build()
    }

    fun removeGeofence(key: String) {
        geofenceList.remove(key)
    }


    fun registerGeofence() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
           return
        }
        geofencingClient.addGeofences(getGeofencingRequest(), pendingIntent)
            .addOnSuccessListener {
                Log.d("GEOFENCE HANDLER", "registerGeofence: SUCCESS")
            }.addOnFailureListener { exception ->
                Log.d("GEOFENCE HANDLER", "registerGeofence: Failure\n$exception")
            }
    }

    suspend fun deregisterGeofence() = kotlin.runCatching {
        geofencingClient.removeGeofences(pendingIntent).await()
        geofenceList.clear()
    }

    fun removeGeofences(){
        geofencingClient?.removeGeofences(pendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                // ...
            }
            addOnFailureListener {
                // Failed to remove geofences
                // ...
            }
        }
    }


}