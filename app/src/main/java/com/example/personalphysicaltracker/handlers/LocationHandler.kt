package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.personalphysicaltracker.activities.LocationInfo
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.ActivityViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocationHandler {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentInfoLocation: LocationInfo = LocationInfo()
    private lateinit var activityViewModel: ActivityViewModel
    private var started = false

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, activityViewModel: ActivityViewModel) {
        fusedLocationClient = client
        this.activityViewModel = activityViewModel
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(30000)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                CoroutineScope(Dispatchers.Default).launch {

                    for (location in locationResult.locations) {
                        checkGeofence(context, location)
                        Log.d("LOCATION HANDLER", "posizione ricevuta: lat = ${location.latitude}, long = ${location.longitude}")

                    }
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates(client: FusedLocationProviderClient) {
        if (fusedLocationClient!=null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
        } else {
            //client.removeLocationUpdates(locationCallback)
        }
    }

    private fun checkGeofence(context: Context, location: Location) {
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val lat = sharedPreferences.getFloat(Constants.GEOFENCE_LATITUDE, 0f).toDouble()
        val lon = sharedPreferences.getFloat(Constants.GEOFENCE_LONGITUDE, 0f).toDouble()
        val radius = Constants.GEOFENCE_RADIUS_IN_METERS.toDouble()

        val geofenceLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        val distance = location.distanceTo(geofenceLocation)

        if (distance <= radius) {
            Log.d("LOCATION HANDLER", "sei nell'area")
            Log.d("LOCATION HANDLER, geo", "$lat;$lon")
            Log.d("LOCATION HANDLER, current", "${location.latitude};${location.longitude}")


            currentInfoLocation.initialize(lat, lon, activityViewModel)
            started = true
        } else {
            Log.d("LOCATION HANDLER", "sei fuori, distante di $distance")

            if (started) {
                if (currentInfoLocation.latitude == lat && currentInfoLocation.longitude==lon) {
                    currentInfoLocation.setFinishTime()
                    started = false
                    Log.d("LOCATION HANDLER", "finito")

                } else { //Si è entrato in un geofence e poi quello è stato modificato
                    started = false
                    currentInfoLocation = LocationInfo()
                }
            }
        }
    }

}
