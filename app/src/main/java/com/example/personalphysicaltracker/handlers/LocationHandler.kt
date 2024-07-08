package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.personalphysicaltracker.services.NotificationServiceActivityRecognition
import com.example.personalphysicaltracker.services.NotificationServiceLocation
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("StaticFieldLeak")
object LocationHandler {

    private lateinit var notificationServiceLocation: NotificationServiceLocation
    private lateinit var context: Context
    private val _isServiceBound = MutableStateFlow(false)
    private val isServiceBound: StateFlow<Boolean> get() = _isServiceBound
    private var isInitialized = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NotificationServiceLocation.SimpleBinderLoc
            notificationServiceLocation = binder.getService()
            _isServiceBound.value = true
            saveServiceState(context, true)
            Log.d("LOCATION HANDLER", "Service connected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _isServiceBound.value = false
            saveServiceState(context, false)
            Log.d("LOCATION HANDLER", "Service disconnected")
        }
    }


    fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, activityViewModel: ActivityDBViewModel) {
        this.context = context

        if (!isServiceRunning(context)) {
            connect()
        } else {
            val bindIntent = Intent(context, NotificationServiceLocation::class.java)
            context.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

        }

        CoroutineScope(Dispatchers.Main).launch {
            isServiceBound.collect { isBound ->
                if (isBound && !isInitialized) {

                    notificationServiceLocation.initialize(activityViewModel, context, client)
                    isInitialized = true
                    Log.d("LOCATION HANDLER", "Service initialized")
                }
            }
        }

    }
    private fun connect(){
        val bindIntent = Intent(context, NotificationServiceLocation::class.java)
        context.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        Log.d("LOCATION HANDLER", "Connection successful")

        context.startService(bindIntent)
        Log.d("LOCATION HANDLER", "Service started")


    }

    fun stopLocationUpdates(stopContext: Context) {
        try {
            if (_isServiceBound.value) {
                val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
                context.stopService(intent)
                Log.d("LOCATION HANDLER", "Service stopped")

                notificationServiceLocation.stopLocationUpdates(context)
                context.unbindService(connection)
                _isServiceBound.value = false
                saveServiceState(context, false)
                Log.d("LOCATION HANDLER", "Service unbound and stopped")
            } else {
                Log.d("LOCATION HANDLER", "Service not bound, cannot unbind")
            }
        } catch (e: Exception) {
            Log.e("LOCATION HANDLER", "An error occurred while stopping", e)
            //bnd and unbind
            val bindIntent = Intent(stopContext, NotificationServiceLocation::class.java)
            stopContext.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
            stopContext.unbindService(connection)
            _isServiceBound.value = false
            saveServiceState(stopContext, false)
        }
    }


    private fun saveServiceState(context: Context, isRunning: Boolean) {
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.SAVE_LOCATION_SERVICE_BINDING, isRunning)
        editor.apply()

    }

    private fun isServiceRunning(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(Constants.SAVE_LOCATION_SERVICE_BINDING, false)
    }




}
