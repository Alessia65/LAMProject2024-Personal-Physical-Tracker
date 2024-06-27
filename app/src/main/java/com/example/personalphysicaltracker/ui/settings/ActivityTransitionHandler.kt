package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.receivers.ActivityTransitionReceiver
import com.example.personalphysicaltracker.ui.home.HomeViewModel
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object ActivityTransitionHandler : LifecycleObserver {

    private lateinit var activityReceiver: ActivityTransitionReceiver
    private lateinit var context: Context
    private lateinit var lifecycle: Lifecycle
    private lateinit var homeViewModel: HomeViewModel


    fun initialize(context: Context, lifecycle: Lifecycle) {
        this.context = context.applicationContext
        this.lifecycle = lifecycle
        activityReceiver = ActivityTransitionReceiver(context, Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION) { userActivity -> }

    }

    private val client by lazy { ActivityRecognition.getClient(context) }

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION_CODE,
            Intent(Constants.BACKGROUND_OPERATION_ACTIVITY_RECOGNITION),
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) PendingIntent.FLAG_CANCEL_CURRENT else PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val activityTransitions = listOf(
        DetectedActivity.STILL,
        DetectedActivity.WALKING,
        DetectedActivity.IN_VEHICLE
    ).flatMap { activityType ->
        listOf(
            ActivityTransition.Builder().setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder().setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
        )
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun registerActivityTransitions() = kotlin.runCatching {
        Log.d("ACTIVITY TRANSITION HANDLER", "mi sto collegando")
        client.requestActivityTransitionUpdates(
            ActivityTransitionRequest(
                activityTransitions
            ), pendingIntent
        ).await()
    }

    @RequiresPermission(Constants.PERMISSION_ACTIVITY_RECOGNITION)
    suspend fun unregisterActivityTransitions() = kotlin.runCatching {
        Log.d("ACTIVITY TRANSITION HANDLER", "mi sto scollegando")

        client.removeActivityUpdates(pendingIntent).await()
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

    fun disconnect(context: Context) {
        lifecycle.removeObserver(activityReceiver)
        lifecycle.removeObserver(this)
        activityReceiver.stopReceiver()
        Log.d("ACTIVITY TRANSITION HANDLER", "OBSERVER OFF")
    }

    fun connect(){
        lifecycle.addObserver(this)
        lifecycle.addObserver(activityReceiver)
        Log.d("ACTIVITY TRANSITION HANDLER", "OBSERVER ON")

    }

    fun handleEvent(event: ActivityTransitionEvent){
        val activityType = event.activityType
        val transitionType= event.transitionType

        if (transitionType.toString() == "START"){

        } else if (transitionType.toString() == "END"){

        } else { //UNKNOWN

        }


    }


}
