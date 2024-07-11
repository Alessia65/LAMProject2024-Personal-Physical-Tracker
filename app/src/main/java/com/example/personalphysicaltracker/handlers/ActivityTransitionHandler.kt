package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.services.NotificationServiceActivityRecognition
import com.example.personalphysicaltracker.viewModels.ActivityHandlerViewModel
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

@SuppressLint("StaticFieldLeak")
object ActivityTransitionHandler {

    lateinit var activityHandlerViewModel: ActivityHandlerViewModel
    private lateinit var context: Context

    private lateinit var notificationService: NotificationServiceActivityRecognition
    private val _isServiceBound = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as NotificationServiceActivityRecognition.SimpleBinderActivity
            notificationService = binder.getService()
            _isServiceBound.value = true
            saveServiceState(context, true)
            Log.d("ACTIVITY TRANSITION HANDLER HANDLER", "Service connected")
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            _isServiceBound.value = false
            saveServiceState(context, false)

            Log.d("ACTIVITY TRANSITION HANDLER HANDLER", "Service disconnected")

        }
    }


    private fun saveServiceState(context: Context, isRunning: Boolean) {
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.SAVE_ACTIVITY_SERVICE_BINDING, isRunning)
        editor.apply()

    }

    private fun isServiceRunning(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(Constants.SAVE_ACTIVITY_SERVICE_BINDING, false)
    }



    fun initialize(context: Context, storeOwner: ViewModelStoreOwner) {
        ActivityTransitionHandler.context = context.applicationContext
        activityHandlerViewModel = ViewModelProvider(storeOwner)[ActivityHandlerViewModel::class.java]
    }

    private val client by lazy { ActivityRecognition.getClient(context) }

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(context, Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION_CODE, Intent(Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION), if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) PendingIntent.FLAG_CANCEL_CURRENT else PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val activityTransitions = listOf(DetectedActivity.STILL, DetectedActivity.WALKING, DetectedActivity.IN_VEHICLE).flatMap { activityType ->
        listOf(
            ActivityTransition.Builder().setActivityType(activityType).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder().setActivityType(activityType).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
        )
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun registerActivityTransitions() = kotlin.runCatching {
        client.requestActivityTransitionUpdates(ActivityTransitionRequest(activityTransitions), pendingIntent).await()
        Log.d("ACTIVITY TRANSITION HANDLER", "Registration successful")
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun unregisterActivityTransitions() = kotlin.runCatching {
        client.removeActivityUpdates(pendingIntent).await()
        Log.d("ACTIVITY TRANSITION HANDLER", "Deletion successful")

    }


    fun getActivityType(int: Int): String = when (int) {
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        else -> "UNKNOWN"
    }

    fun getTransitionType(int: Int): String = when (int) {
        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "START"
        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "END"
        else -> "UNKNOWN"
    }

    fun disconnect() {
        try {
            notificationService.checkActivityToStop()
            if (isServiceRunning(context)) {
                val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
                context.stopService(intent)
                notificationService.stopAll()
                context.unbindService(serviceConnection)
                _isServiceBound.value = false
                saveServiceState(context, false)

                Log.d("ACTIVITY TRANSITION HANDLER", "Service unbound and stopped")
            } else {
                val intent = Intent(context, NotificationServiceActivityRecognition::class.java)
                context.stopService(intent)
                notificationService.stopAll()
                Log.d("ACTIVITY TRANSITION HANDLER", "Service not bound, cannot unbind")
            }

        }catch (e: Exception){
            Log.d("ACTIVITY TRANSITION HANDLER", "error occurred while disconnecting")
        }

    }

    fun connect(){
        val bindIntent = Intent(context, NotificationServiceActivityRecognition::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d("ACTIVITY TRANSITION HANDLER", "Connection successful")

        context.startService(bindIntent)
        Log.d("ACTIVITY TRANSITION HANDLER", "Service started")


    }


    fun setCurrentActivity(activity: PhysicalActivity){
        notificationService.selectedActivity = activity
    }

    fun getCurrentActivity(): PhysicalActivity {
        return notificationService.selectedActivity
    }



}
