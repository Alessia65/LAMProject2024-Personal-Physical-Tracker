package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.personalphysicaltracker.utils.Constants


@SuppressLint("StaticFieldLeak")
object PermissionsHandler {

    var notificationPermission = false
    var locationPermission = false

    private var ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = ""

    fun checkPermissions(context: Context): Boolean{
        val activityRecognise: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            activityRecognise = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION)
            ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = Constants.PERMISSION_ACTIVITY_RECOGNITION
        } else {
            activityRecognise = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION_BEFORE)
            ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = Constants.PERMISSION_ACTIVITY_RECOGNITION_BEFORE
            Log.d("PERMISSION", "Old permission")
        }

        val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        return activityRecognise == PackageManager.PERMISSION_GRANTED && postNotifications == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: Activity) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                ACTUAL_PERMISSION_ACTIVITY_RECOGNITION,
                Constants.PERMISSION_POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                ACTUAL_PERMISSION_ACTIVITY_RECOGNITION
            )
        }

        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest,
            Constants.PERMISSION_REQUESTS_CODE
        )
    }


    fun hasLocationPermissions(context: Context): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            locationPermission =
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.INTERNET) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            locationPermission =
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.INTERNET) == PackageManager.PERMISSION_GRANTED)
        } else { // For safety
            locationPermission =
                (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(context, Constants.INTERNET) == PackageManager.PERMISSION_GRANTED)
        }
        return locationPermission
    }

    fun requestLocationPermissions(activity: Activity, context: Context): Boolean {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Constants.ACCESS_FINE_LOCATION,
                Constants.ACCESS_BACKGROUND_LOCATION,
                Constants.ACCESS_NETWORK_STATE,
                Constants.INTERNET,
                Constants.FOREGROUND_SERVICE_LOCATION)
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Constants.ACCESS_FINE_LOCATION,
                Constants.ACCESS_BACKGROUND_LOCATION,
                Constants.ACCESS_NETWORK_STATE,
                Constants.INTERNET)
        } else {  //For safety
            arrayOf(
                Constants.ACCESS_FINE_LOCATION,
                Constants.ACCESS_COARSE_LOCATION,
                Constants.ACCESS_NETWORK_STATE,
                Constants.INTERNET
            )
        }

        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest,
            Constants.PERMISSION_LOCATION_REQUESTS_CODE
        )

        return hasLocationPermissions(context)
    }

}
