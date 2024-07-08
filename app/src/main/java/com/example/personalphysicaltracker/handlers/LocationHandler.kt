package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.personalphysicaltracker.services.NotificationServiceLocation
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

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NotificationServiceLocation.SimpleBinderLoc
            notificationServiceLocation = binder.getService()
            _isServiceBound.value = true
            Log.d("LOCATION HANDLER", "Service connected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _isServiceBound.value = false
            Log.d("LOCATION HANDLER", "Service disconnected")
        }
    }

    fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, activityViewModel: ActivityDBViewModel) {
        this.context = context
        val serviceIntent = Intent(context, NotificationServiceLocation::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        CoroutineScope(Dispatchers.Main).launch {
            isServiceBound.collect { isBound ->
                if (isBound) {
                    notificationServiceLocation.initialize(activityViewModel, context, client)
                    Log.d("LOCATION HANDLER", "Service initialized")
                }
            }
        }
    }

    fun stopLocationUpdates(stopContext: Context) {
        try {
            if (_isServiceBound.value) {
                notificationServiceLocation.stopLocationUpdates(stopContext)
                stopContext.unbindService(connection)
                _isServiceBound.value = false
                Log.d("LOCATION HANDLER", "Service unbound and stopped")
            } else {
                Log.d("LOCATION HANDLER", "Service not bound, cannot unbind")
            }
        } catch (e: Exception){
            Log.e("LOCATION HANDLER", "An error occurred while stopping", e)
        }
    }
}
