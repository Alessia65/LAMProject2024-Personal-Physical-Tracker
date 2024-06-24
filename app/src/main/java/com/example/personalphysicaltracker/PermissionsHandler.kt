package com.example.personalphysicaltracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@SuppressLint("StaticFieldLeak")
object PermissionsHandler {

    private lateinit var context: Context


    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun checkBuildVersionQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    fun checkBuildVersionO(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissionActivityRecognition(activity:Activity) {
        if (!isPermissionGranted(android.Manifest.permission.ACTIVITY_RECOGNITION)) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), Constants.ACTIVITY_RECOGNITION_REQUEST_CODE)
        }
    }

    fun requestPermissionNotifications(activity: Activity){
        if (!isPermissionGranted(android.Manifest.permission.POST_NOTIFICATIONS)) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), Constants.NOTIFICATION_PERMISSION_REQUEST_CODE)
        }
    }

}
