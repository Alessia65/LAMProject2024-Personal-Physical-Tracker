package com.example.personalphysicaltracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@SuppressLint("StaticFieldLeak")
object PermissionsHandler {

    public  var notificationPermission = false
    fun checkPermissions(context: Context): Boolean{
        val activityRecognise = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION)
        val postNotifications = ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_POST_NOTIFICATIONS)
        return activityRecognise == PackageManager.PERMISSION_GRANTED && postNotifications == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: Activity){
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Constants.PERMISSION_ACTIVITY_RECOGNITION, Constants.PERMISSION_POST_NOTIFICATIONS),
            Constants.PERMISSION_REQUESTS_CODE
        )
    }


}
