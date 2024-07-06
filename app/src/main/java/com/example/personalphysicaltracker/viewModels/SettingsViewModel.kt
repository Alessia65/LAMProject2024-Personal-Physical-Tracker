package com.example.personalphysicaltracker.viewModels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.GeofenceHandler
import com.example.personalphysicaltracker.handlers.LocationHandler
import com.example.personalphysicaltracker.handlers.NotificationHandler
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityDBViewModel

    private fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {

        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
    }

    private fun getActivityViewModel(): ActivityDBViewModel{
        return activityViewModel
    }

    fun getTotalStepsFromToday(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner, callback: (Long) -> Unit) {
        initializeActivityViewModel(activity, viewModelStoreOwner)
        val today = getCurrentDay()
        viewModelScope.launch {
            val totalSteps = try {
                withContext(Dispatchers.IO) {

                    activityViewModel.getTotalStepsFromToday(today)
                }

            } catch (e: Exception) {
                // Exception handling, such as logging or other types of handling
                Log.e("SettingsViewModel", "Exception with total steps = 0")
                0
            }
            callback(totalSteps)
        }
    }

    fun scheduleStepsNotification(selectedNumber: Int, context: Context) {
         val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
         val editor = sharedPreferences.edit()
         editor.putInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, selectedNumber)
         editor.apply()

         NotificationHandler.scheduleStepsNotificationIfEnabled(context)

    }

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int, formattedTime: String) {

        //Set sharedPreferences:
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, hour)
        editor.putInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, minute)
        editor.putString(Constants.SHARED_PREFERENCES_DAILY_REMINDER_FORMATTED_TIME, formattedTime)
        editor.apply()

        NotificationHandler.scheduleDailyNotificationIfEnabled(context)

    }
    fun cancelStepsNotification(context: Context){
        NotificationHandler.cancelStepsNotification(context)
    }

    fun cancelDailyNotification(context: Context) {
         NotificationHandler.cancelDailyNotification(context)
     }

    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun connectToActivityTransition(context: Context){
        ActivityTransitionHandler.connect()
        registerActivityTransitions(context)
    }

    private fun registerActivityTransitions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                ActivityTransitionHandler.registerActivityTransitions()
            }
        }
    }

    fun disconnect(context: Context){
        ActivityTransitionHandler.disconnect()
        unregisterActivityTransitions(context)
    }

    private fun unregisterActivityTransitions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                ActivityTransitionHandler.unregisterActivityTransitions()
            }
        }
    }

    fun startLocationUpdates(context: Context){
        LocationHandler.startLocationUpdates(context, LocationServices.getFusedLocationProviderClient(context), getActivityViewModel())
    }

    fun stopLocationUpdates(context: Context){
        LocationHandler.stopLocationUpdates(LocationServices.getFusedLocationProviderClient(context))
    }

    fun deregisterGeofence(){
        viewModelScope.launch(Dispatchers.IO) {
            GeofenceHandler.deregisterGeofence()
        }
    }

    fun removeGeofence(){
        GeofenceHandler.removeGeofence("selected_location")
    }


    //Shared Preferences
    fun checkBackgroundRecogniseActivitiesOn(context: Context): Boolean{
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        return  (sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false))
    }
    fun setBackgroundRecogniseActivities(context: Context, isChecked: Boolean){
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, isChecked)
        editor.apply()
    }

    fun checkBackgroundLocationDetection(context: Context): Boolean {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        return  (sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false))
    }

    fun setBackgroundLocationDetection(context: Context, isChecked: Boolean) {
        val sharedPreferencesBackgroundActivities = context.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val editor = sharedPreferencesBackgroundActivities.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, isChecked)
        editor.apply()
    }


    fun checkDailyReminder(context: Context): Boolean{
        val sharedPreferencesDaily = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        return sharedPreferencesDaily.getBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)
    }

    fun setDailyReminder(context: Context, value: Boolean){
        val sharedPreferencesDaily = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferencesDaily.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, value)
        editor.apply()
    }

    fun checkStepsReminder(context: Context): Boolean{
        val sharedPreferencesSteps = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        return sharedPreferencesSteps.getBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)

    }

    fun setStepsReminder(context: Context, value: Boolean){
        val sharedPreferencesSteps = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val editor = sharedPreferencesSteps.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, value)
        editor.apply()
    }

    fun checkLocationSet(context: Context): Boolean{
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val key = sharedPreferences.getString(Constants.GEOFENCE_KEY, null)
        return !key.isNullOrEmpty()
    }


    fun obtainGeofenceKey(context: Context): String?{
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        return sharedPreferences.getString(Constants.GEOFENCE_KEY, null)
    }

    fun obtainGeofenceLatitude(context: Context): Double{
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat(Constants.GEOFENCE_LATITUDE, 0.0f).toDouble()
    }

    fun obtainGeofenceLongitude(context: Context): Double{
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat(Constants.GEOFENCE_LONGITUDE, 0.0f).toDouble()
    }

    fun editGeofencePreferences(key: String, location: Location, context: Context){
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(Constants.GEOFENCE_KEY, key)
            putFloat(Constants.GEOFENCE_LATITUDE, location.latitude.toFloat())
            putFloat(Constants.GEOFENCE_LONGITUDE, location.longitude.toFloat())
            apply()
        }
    }

    fun removeGeofencePreferences(context: Context){
        val sharedPreferences = context.getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        setBackgroundLocationDetection(context, false)
        stopLocationUpdates(context)
        deregisterGeofence()
        removeGeofence()
    }

    //Geofence
    fun initializeGeofenceHandler(context: Context){
        GeofenceHandler.initialize(context)
    }

    fun removeGeofence(value: String){
        GeofenceHandler.removeGeofence(value)
    }

    fun addGeofence(key: String, location: Location){
        GeofenceHandler.addGeofence(key, location)
    }

    fun registerGeofence(){
        GeofenceHandler.registerGeofence()
    }
}