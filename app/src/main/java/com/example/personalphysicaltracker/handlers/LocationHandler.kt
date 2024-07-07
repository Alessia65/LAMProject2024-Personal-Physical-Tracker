package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.personalphysicaltracker.activities.LocationInfo
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.services.NotificationServiceLocation
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object LocationHandler {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var activityDBViewModel: ActivityDBViewModel
    private var firstOpen = true
    private lateinit var context: Context
    private lateinit var notificationServiceLocation: NotificationServiceLocation


    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, activityViewModel: ActivityDBViewModel) {
        this.context = context
        notificationServiceLocation = NotificationServiceLocation()
        notificationServiceLocation.initialize(activityViewModel, context, client)
        if (firstOpen) {
            startForegroundService(context)
            firstOpen = false
        }
        fusedLocationClient = client
        this.activityDBViewModel = activityViewModel
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(30000)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                CoroutineScope(Dispatchers.Default).launch {
                    for (location in locationResult.locations) {
                        notificationServiceLocation.checkGeofence(location)
                        Log.d("LOCATION HANDLER", "Position received: lat = ${location.latitude}, long = ${location.longitude}")
                    }
                }
            }
        }


        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates(stopContext: Context) {
        try {
            if (fusedLocationClient != null) {
                fusedLocationClient?.removeLocationUpdates(locationCallback)
                stopForegroundService(context)
                notificationServiceLocation.stopPermanentNotificationLocationDetection()
            } else {
                Log.e("LOCATION HANDLER", "Client null")
                setBackgroundLocationDetection(stopContext, false)
            }
        }catch (e: Exception){
            Log.e("LOCATION HANDLER", "Error occurred")
            setBackgroundLocationDetection(stopContext, false)
        }

    }

    private fun setBackgroundLocationDetection(context: Context, isChecked: Boolean) {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, isChecked)
        editor.apply()
    }

    private fun startForegroundService(context: Context) {
        val serviceIntent = Intent(context, NotificationServiceLocation::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, NotificationServiceLocation::class.java)
        context.stopService(serviceIntent)
    }




}
