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

    fun checkActivityRecognitionPermission(context: Context): Boolean {
        return (ActivityCompat.checkSelfPermission(context, Constants.PERMISSION_ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)

    }

    fun shouldShowRequest(activity: AppCompatActivity): Boolean{
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity, Constants.PERMISSION_ACTIVITY_RECOGNITION))
    }

    fun requestActivityRecognitionPermission(activity: AppCompatActivity){
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Constants.PERMISSION_ACTIVITY_RECOGNITION),
            Constants.ACTIVITY_RECOGNITION_REQUEST_CODE
        )
    }

    fun checkGrantResults(grantResults: IntArray): Boolean{
        return (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)

    }
}
