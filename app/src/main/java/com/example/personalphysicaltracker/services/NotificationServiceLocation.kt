package com.example.personalphysicaltracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.personalphysicaltracker.MainActivity
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.LocationInfo
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationServiceLocation : Service() {

    private val binder = SimpleBinderLoc()
    private lateinit var context: Context
    private var currentInfoLocation: LocationInfo = LocationInfo()
    private lateinit var activityDBViewModel: ActivityDBViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    inner class SimpleBinderLoc : Binder() {
        fun getService(): NotificationServiceLocation {
            return this@NotificationServiceLocation
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    fun initialize(activityDBViewModel: ActivityDBViewModel, context: Context, fusedLocationClient: FusedLocationProviderClient){


        this.activityDBViewModel = activityDBViewModel
        this.context = context
        this.fusedLocationClient = fusedLocationClient

        createNotificationChannelForLocationDetection()
        createPermanentNotificationLocationDetection()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(30000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                CoroutineScope(Dispatchers.Default).launch {
                    for (location in locationResult.locations) {
                        checkGeofence(location)
                        Log.d("LOCATION HANDLER", "Position received: lat = ${location.latitude}, long = ${location.longitude}")
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun createNotificationChannelForLocationDetection() {
        val channelId = Constants.CHANNEL_LOCATION_DETECTION_ID
        val nameLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_TITLE
        val descriptionLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_DESCRIPTION
        val importanceLocationDetectionChannel = NotificationManager.IMPORTANCE_HIGH
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        var channelLocationDetection = notificationManager.getNotificationChannel(channelId)
        if (channelLocationDetection == null) {
            channelLocationDetection = NotificationChannel(
                    channelId,
                    nameLocationDetectionChannel,
                    importanceLocationDetectionChannel
                ).apply {
                    description = descriptionLocationDetectionChannel
                }
            notificationManager.createNotificationChannel(channelLocationDetection)

        } else {
            Log.d("NotificationChannel", "Channel $channelId already exists")
        }


    }

    private fun createNotification(): Notification {


        return NotificationCompat.Builder(context, Constants.CHANNEL_LOCATION_DETECTION_ID)
            .setContentTitle("Location Detection On")
            .setContentText("Don't close the app.")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(null)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createPermanentNotificationLocationDetection() {
        val notification = createNotification()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NOTIFICATION SERVICE", "Permission not Granted")
            return
        }

        Log.d("NOTIFICATION SERVICE LOCATION", "Starting")
        startForeground(Constants.REQUEST_CODE_LOCATION_DETECTION, notification)

    }

    private fun showLocationChangesNotification(title: String, message: String) {

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val notification = NotificationCompat.Builder(context,
            Constants.CHANNEL_LOCATION_DETECTION_ID
        )
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(null)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(this.context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NOTIFICATION SERVICE", "Permission not Granted")
                return
            }
            notify(Constants.REQUEST_CODE_LOCATION_DETECTION, notification)
        }
    }

    private fun stopPermanentNotificationLocationDetection() {
        deleteChannelLocationDetection()
        val notificationManager = this.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.REQUEST_CODE_LOCATION_DETECTION)
        Log.d("NOTIFICATION SERVICE LOCATION", "CHANNEL DELETED")

    }

    private fun deleteChannelLocationDetection() {
        val notificationManager = this.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.REQUEST_CODE_LOCATION_DETECTION)
        Log.d("NOTIFICATION SERVICE LOCATION", "deleteChannelLocationDetection")
    }




    fun checkGeofence(location: Location) {
        val sharedPreferences = this.context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
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
                showLocationChangesNotification( "Your location changed", "you have entered your area of interest")
                currentInfoLocation.initialize(lat, lon, activityDBViewModel
                )
                setIsEntered(true)
                sharedPreferences.edit().putBoolean(Constants.GEOFENCE_IS_INSIDE, true).apply()
                sharedPreferences.edit().putString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start).apply()

            } else {

                showLocationChangesNotification("Reminder", "you are in  your area of interest") //For testing

                currentInfoLocation.initialize(lat, lon, activityDBViewModel)
                val oldStart = sharedPreferences.getString(Constants.GEOFENCE_ENTRANCE, currentInfoLocation.start).toString()
                currentInfoLocation.start = oldStart
                currentInfoLocation.date = oldStart.substring(0,10)
                setIsEntered(true)
            }
        } else {
            Log.d("LOCATION HANDLER", "You are out of your area of interest, you are distant: $distance")

            if (checkIsEntered()) {
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
                        if (!currentInfoLocation.saved) {
                            currentInfoLocation.saveInDb()
                            currentInfoLocation.saved = true
                        }

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
                    setIsEntered(false)
                    showLocationChangesNotification( "Your location changed", "you are out of your area of interest")

                } else { //If you entered a geofence and then it was modified
                    setIsEntered(false)
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

    private fun checkIsEntered(): Boolean{
        val sharedPreferences = this.context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(Constants.GEOFENCE_IS_ENTERED, false)

    }

    private fun setIsEntered(value: Boolean){
        val sharedPreferences = this.context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.GEOFENCE_IS_ENTERED, value)
        editor.apply()
    }

    fun stopLocationUpdates(stopContext: Context) {
        try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                stopForegroundService()
                stopPermanentNotificationLocationDetection()
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

    private fun stopForegroundService() {
        val serviceIntent = Intent(this.context, NotificationServiceLocation::class.java)
        this.context.stopService(serviceIntent)
    }

    override fun onDestroy() {
        stopPermanentNotificationLocationDetection()
        stopForegroundService()
        super.onDestroy()
    }

}
