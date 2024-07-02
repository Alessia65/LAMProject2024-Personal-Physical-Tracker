package com.example.personalphysicaltracker.handlers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.receivers.ActivityTransitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("StaticFieldLeak")
object ActivityTransitionHandler : LifecycleObserver {

    private lateinit var activityReceiver: ActivityTransitionReceiver
    private lateinit var context: Context
    private lateinit var lifecycle: Lifecycle
    private var started = false
    private var stopped = false
    private var isWalkingType  = false

    fun initialize(context: Context, lifecycle: Lifecycle) {
        ActivityTransitionHandler.context = context.applicationContext
        ActivityTransitionHandler.lifecycle = lifecycle
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

    fun disconnect() {
        Log.d("ON DESTROY", "In disconnect()")
        //Prima devo concludere l'attività se è running
        if (started){
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    ActivityHandler.stopSelectedActivity(isWalkingType)
                }
                started = false
                stopped = true
                Log.d("ON DESTROY", "In disconnect() try")
            }catch (e: Exception){
                Log.d("EXCEPTION CATCHED", e.message.toString())
            }
        }
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

    fun handleEvent(activityType: String, transitionType: String){
        isWalkingType = false
        lateinit var activity: PhysicalActivity
        when (activityType) {
            "WALKING" -> {
                activity = WalkingActivity()
                isWalkingType = true
            }
            "IN_VEHICLE" -> {
                activity = DrivingActivity()
            }
            "STILL" -> {
                activity = StandingActivity()
            }
            else -> {
                activity = PhysicalActivity()
            }
        }


        if (transitionType == "START"){
            CoroutineScope(Dispatchers.IO).launch{
                if (stopped) {
                    ActivityHandler.startSelectedActivity(activity)
                    started = true
                    stopped = false
                } else {
                    try {
                        ActivityHandler.stopSelectedActivity(isWalkingType)
                        stopped = true
                    } catch (e: Exception){
                        //Concludo attività
                        Log.d("MESSAGGIO DI ERRORE", e.message.toString()) //COSA SUCCEDE?
                    } finally {
                        ActivityHandler.startSelectedActivity(activity)
                        started = true
                    }

                }
            }
            // Attendere il completamento della coroutine
            Thread.sleep(1000)

        } else if (transitionType.toString() == "END" ){

            if (started) {
                CoroutineScope(Dispatchers.IO).launch {
                    ActivityHandler.stopSelectedActivity(isWalkingType)
                    started = false
                    stopped = true
                }
                // Attendere il completamento della coroutine
                Thread.sleep(1000)
            } else {
                Log.d("ACTIVITY TRANSITION HANDLER", "no activity to stop")
                return
            }

        } else { //UNKNOWN
            return
        }
    }




}
