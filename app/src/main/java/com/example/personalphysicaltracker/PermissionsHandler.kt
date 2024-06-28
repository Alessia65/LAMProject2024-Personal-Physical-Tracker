package com.example.personalphysicaltracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@SuppressLint("StaticFieldLeak")
object PermissionsHandler {

    public  var notificationPermission = false
    public  var locationPermission = false

    var ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = ""
    fun checkPermissions(context: Context): Boolean{
        var activityRecognise = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            activityRecognise = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION)
            ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = Constants.PERMISSION_ACTIVITY_RECOGNITION
            Log.d("PERMISSION", "normale")
        } else {
            activityRecognise = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION_BEFORE)
            ACTUAL_PERMISSION_ACTIVITY_RECOGNITION = Constants.PERMISSION_ACTIVITY_RECOGNITION_BEFORE
            Log.d("PERMISSION", "pazzo")

        }
        val postNotifications = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_POST_NOTIFICATIONS)
        return activityRecognise == PackageManager.PERMISSION_GRANTED && postNotifications == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: Activity){
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(ACTUAL_PERMISSION_ACTIVITY_RECOGNITION, Constants.PERMISSION_POST_NOTIFICATIONS),
            Constants.PERMISSION_REQUESTS_CODE
        )
    }

    fun hasLocationPermissions(context: Context): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            locationPermission =  (ActivityCompat.checkSelfPermission(
                context,
                Constants.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        } else {
            locationPermission =  (ActivityCompat.checkSelfPermission(context, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Constants.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        }
        return locationPermission
    }

    fun requestLocationPermissions(activity: Activity, context: Context): Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Constants.ACCESS_BACKGROUND_LOCATION),
                Constants.PERMISSION_LOCATION_REQUESTS_CODE
            )
        }
        else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Constants.ACCESS_FINE_LOCATION, Constants.ACCESS_COARSE_LOCATION),
                Constants.PERMISSION_LOCATION_REQUESTS_CODE
            )
        }
        return hasLocationPermissions(context)
    }


}
