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
                        Log.d("LOCATION HANDLER", "Position received: lat = ${location.latitude}, long = ${location.longitude}")
                    }
                }
            }
        }


        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates(client: FusedLocationProviderClient) {
        if (fusedLocationClient != null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
        } else {
            Log.e("LOCATION HANDLER", "Client null")
            client.removeLocationUpdates(locationCallback)
        }
        stopForegroundService(context)
        notificationServiceLocation.stopPermanentNotificationLocationDetection()
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
                Log.d("LOCATION HANDLER", "You are in your interest's area")
                Log.d("LOCATION HANDLER", "Geofence set in: $lat;$lon")
                Log.d("LOCATION HANDLER", "Currently: ${location.latitude};${location.longitude}")
                notificationServiceLocation.showLocationChangesNotification(context, "Your location changed", "you have entered your area of interest")
                currentInfoLocation.initialize(lat, lon, activityDBViewModel)
                started = true
                sharedPreferences.edit().putBoolean(Constants.GEOFENCE_IS_INSIDE, true).apply()
                sharedPreferences.edit().putString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start).apply()

            } else {
                /*
                    If I was already inside we remember what time I entered because if the app restarts it no longer knows
                    Let's save the time we entered.
                    The latitude and longitude will not be the same but the important thing is that we were within the area
                 */
                notificationServiceLocation.showLocationChangesNotification(context, "Reminder", "you are in  your area of interest")

                currentInfoLocation.initialize(lat, lon, activityDBViewModel)
                val oldStart = sharedPreferences.getString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start).toString()
                currentInfoLocation.start = oldStart
                currentInfoLocation.date = oldStart.substring(0,10)
                started = true
            }
        } else {
            Log.d("LOCATION HANDLER", "You are out of your area of interest, you are distant: $distance")

            if (started) {
                if (currentInfoLocation.latitude == lat && currentInfoLocation.longitude == lon) {


                    // If the activity starts before midnight and ends the next day
                    val currentTime = Calendar.getInstance().time
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val startTime = dateFormat.parse(currentInfoLocation.start) ?: return

                    val endOfDay = getEndOfToday(startTime)
                    if (startTime.before(getStartOfToday(currentTime))) {
                        val endOfDayString = dateFormat.format(endOfDay)
                        currentInfoLocation.setFinishTimeWithString(endOfDayString)
                        currentInfoLocation.calculateDuration()
                        currentInfoLocation.saveInDb()

                        val locationInfoNew = LocationInfo()
                        locationInfoNew.initialize(lat, lon, activityDBViewModel)
                        currentInfoLocation = locationInfoNew

                        val startOfNextDay = Calendar.getInstance().apply {
                            time = endOfDay
                            add(Calendar.SECOND, 1)
                        }.time

                        val startOfDayString = dateFormat.format(startOfNextDay)
                        currentInfoLocation.setStartTimeWithString(startOfDayString)
                    }


                    currentInfoLocation.setFinishTime()
                    started = false
                    notificationServiceLocation.showLocationChangesNotification(context, "Your location changed", "you are out of your area of interest")

                } else { //If you entered a geofence and then it was modified
                    started = false
                    currentInfoLocation = LocationInfo()
                }
            }
            sharedPreferences.edit().putBoolean(Constants.GEOFENCE_IS_INSIDE, false).apply()
        }
    }

    private fun getStartOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfToday(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }


}
