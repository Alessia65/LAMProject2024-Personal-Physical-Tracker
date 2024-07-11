package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.personalphysicaltracker.utils.Constants


@SuppressLint("StaticFieldLeak")
object PermissionsHandler {

    fun checkPermissionsActivityRecognition(context: Context): Boolean{
        val activityRecognise: Int = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION)
        return activityRecognise == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissionNotifications(context: Context): Boolean{
        val postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        return postNotifications == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissionsActivity(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Constants.PERMISSION_ACTIVITY_RECOGNITION),
            Constants.PERMISSION_REQUESTS_CODE_ACTIVITY
        )
    }

    fun requestPermissionNotifications(activity: Activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Constants.PERMISSION_POST_NOTIFICATIONS),
                Constants.PERMISSION_REQUESTS_CODE_NOTIFICATION
            )
        }

    }



    fun hasLocationPermissions(context: Context): Boolean {
        val locationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.INTERNET) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        } else{
            (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(context, Constants.INTERNET) == PackageManager.PERMISSION_GRANTED)
        }
        return locationPermission
    }

    fun requestLocationPermissions(activity: Activity): Boolean {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Constants.ACCESS_FINE_LOCATION,
                Constants.ACCESS_BACKGROUND_LOCATION,
                Constants.ACCESS_NETWORK_STATE,
                Constants.INTERNET,
                Constants.FOREGROUND_SERVICE_LOCATION)
        } else {
            arrayOf(
                Constants.ACCESS_FINE_LOCATION,
                Constants.ACCESS_BACKGROUND_LOCATION,
                Constants.ACCESS_NETWORK_STATE,
                Constants.INTERNET)
        }

        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest,
            Constants.PERMISSION_LOCATION_REQUESTS_CODE
        )

        return hasLocationPermissions(activity)
    }

}
