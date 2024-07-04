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

@SuppressLint("StaticFieldLeak")
// In LocationHandler.kt

object LocationHandler {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentInfoLocation: LocationInfo = LocationInfo()
    private lateinit var activityDBViewModel: ActivityDBViewModel
    private var started = false
    private var firstOpen = true
    private lateinit var context: Context
    private lateinit var notificationServiceLocation: NotificationServiceLocation


    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, activityViewModel: ActivityDBViewModel) {
        this.context = context
        notificationServiceLocation = NotificationServiceLocation()
        if (firstOpen) {

            startForegroundService(context)

            Log.d("LOCATION HANDLER", "FIRST OPEN")
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
                        checkGeofence(context, location)
                        Log.d("LOCATION HANDLER", "posizione ricevuta: lat = ${location.latitude}, long = ${location.longitude}")
                    }
                }
            }
        }


        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates(client: FusedLocationProviderClient) {
        if (fusedLocationClient != null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
            stopForegroundService(context)
            notificationServiceLocation.stopPermanentNotificationLocationDetection()
        } else {
            // Handle the case when fusedLocationClient is null
        }
    }

    private fun startForegroundService(context: Context) {
        val serviceIntent = Intent(context, NotificationServiceLocation::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, NotificationServiceLocation::class.java)
        context.stopService(serviceIntent)
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
            if (!sharedPreferences.getBoolean(Constants.GEOFENCE_IS_INSIDE, false)) {
                Log.d("LOCATION HANDLER", "sei nell'area")
                Log.d("LOCATION HANDLER, geo", "$lat;$lon")
                Log.d("LOCATION HANDLER, current", "${location.latitude};${location.longitude}")
                notificationServiceLocation.showLocationChangesNotification(context, "Your location changed", "you have entered your area of interest")
                currentInfoLocation.initialize(lat, lon, activityDBViewModel)
                started = true
                sharedPreferences.edit().putBoolean(Constants.GEOFENCE_IS_INSIDE, true).apply()
                sharedPreferences.edit().putString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start).apply()

            } else { //Se ero gia dentro ricordiamo a che ora ero entrato perchè se l'app si riavvia non lo sa piu
                /*
                    Risalviamo l'orario in cui eravamo entrati.
                    La lat e long non saranno uguali ma l'importante è che eravamo all'interno dell'area
                 */
                notificationServiceLocation.showLocationChangesNotification(context, "Reminder", "you are in  your area of interest")

                currentInfoLocation.initialize(lat, lon, activityDBViewModel)
                val t = sharedPreferences.getString(Constants.GEOFENCE_ENTRANCE, "")
                Log.d("START PRIMA", t.toString())
                currentInfoLocation.start =
                    sharedPreferences.getString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start)
                        .toString() //SE non lo trova lo risetta
                Log.d("PROVA", currentInfoLocation.start)
                started = true
            }
        } else {
            Log.d("LOCATION HANDLER", "sei fuori, distante di $distance")

            if (started) {
                if (currentInfoLocation.latitude == lat && currentInfoLocation.longitude == lon) {
                    currentInfoLocation.setFinishTime()
                    started = false
                    Log.d("LOCATION HANDLER", "finito")
                    notificationServiceLocation.showLocationChangesNotification(context, "Your location changed", "you are out of your area of interest")

                } else { //Si è entrato in un geofence e poi quello è stato modificato
                    started = false
                    currentInfoLocation = LocationInfo()
                }
            }
            sharedPreferences.edit().putBoolean(Constants.GEOFENCE_IS_INSIDE, false).apply()
        }
    }


}
