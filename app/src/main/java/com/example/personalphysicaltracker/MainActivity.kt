package com.example.personalphysicaltracker

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalphysicaltracker.handlers.ActivityHandler
import com.example.personalphysicaltracker.databinding.ActivityMainBinding
import com.example.personalphysicaltracker.receivers.ActivityTransitionReceiver
import com.example.personalphysicaltracker.handlers.AccelerometerSensorHandler
import com.example.personalphysicaltracker.handlers.StepCounterSensorHandler
import com.example.personalphysicaltracker.receivers.DailyReminderReceiver
import com.example.personalphysicaltracker.receivers.StepsReminderReceiver
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.LocationHandler
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.utils.NotificationServiceActivityRecognition
import com.example.personalphysicaltracker.viewModels.ActivityViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activityTransitionHandler: ActivityTransitionHandler
    private lateinit var activityReceiver: ActivityTransitionReceiver


    // ActivityResultLauncher to handle permission request result
    private val SETTINGS_PERMISSION_REQUEST = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Check if activity recognition permission is granted
        if (ActivityCompat.checkSelfPermission(this, Constants.PERMISSION_ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Constants.PERMISSION_POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                showDialogSettingsNotification()
                PermissionsHandler.notificationPermission = false
            } else {
                PermissionsHandler.notificationPermission = true
            }
        } else {
            // Show settings dialog if permission is still not granted
            showDialogSettingsActivityRecognition()
        }
    }




    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request activity recognition permission if not granted
        //requestActivityRecnognisePermission()

        if (!PermissionsHandler.checkPermissions(getApplicationContext())){
            PermissionsHandler.requestPermissions(this)
        } else {
            PermissionsHandler.notificationPermission = true
        }

        // Initialize Sensors (DONT DELETE)
        val accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(this)
        val stepCounterSensorHandler = StepCounterSensorHandler.getInstance(this)

        // Initialize navigation components
        initializeNavigation()

        // Create notification channel if necessary
        createNotificationChannels()

        // Schedule daily notification if enabled
        scheduleDailyNotificationIfEnabled()

        // Schedule steps notification if enabled
        scheduleStepsNotificationIfEnabled()

        //val sharedPreferencesBackgroundActivities = this.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        //val  backgroundRecognitionEnabled = sharedPreferencesBackgroundActivities.getBoolean( Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false)
        initializeObserverActivityTransition()
        /*
        val notificationServiceActivityRecognition = NotificationServiceActivityRecognition()
        if (backgroundRecognitionEnabled){
            //ActivityTransitionHandler.connect()
            //notificationServiceActivityRecognition.createNotificationChannelForActivityRecognition()
        } else {
            //notificationServiceActivityRecognition.stopPermanentNotificationActivityRecognition()


        }

         */


        /*
        val sharedPreferencesBackgroundLocation = this.getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val  backgroundLocationEnabled = sharedPreferencesBackgroundLocation.getBoolean(
            Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false)
        createNotificationChannelForLocationDetection()
        Log.d("ENABLED?", "$backgroundLocationEnabled")
        if (backgroundLocationEnabled){
            Log.d("AHH", "ENABLED")
            LocationHandler.startLocationUpdates(this, LocationServices.getFusedLocationProviderClient(this), ViewModelProvider(this)[ActivityViewModel::class.java])
        }

         */

    }





/*
    private fun createNotificationChannelForLocationDetection() {
        val nameLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_TITLE
        val descriptionLocationDetectionChannel = Constants.CHANNEL_LOCATION_DETECTION_DESCRIPTION
        val importanceLocationDetectionChannel = NotificationManager.IMPORTANCE_HIGH
        val channeLocationDetection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(Constants.CHANNEL_LOCATION_DETECTION_ID, nameLocationDetectionChannel, importanceLocationDetectionChannel).apply {
                description = descriptionLocationDetectionChannel
            }
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        val notificationManagerLocationDetection: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManagerLocationDetection.createNotificationChannel(channeLocationDetection)
    }*/



    private fun createNotificationChannels() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NOTIFICATION CHANNEL", "channels created by MainActivity")
            //Daily Reminder
            val nameDailyReminder = Constants.CHANNEL_DAILY_REMINDER_TITLE
            val descriptionTextDailyReminder = Constants.CHANNEL_DAILY_REMINDER_DESCRIPTION
            val importanceDailyReminder = NotificationManager.IMPORTANCE_HIGH
            val channelDailyReminder = NotificationChannel(Constants.CHANNEL_DAILY_REMINDER_ID, nameDailyReminder, importanceDailyReminder).apply {
                description = descriptionTextDailyReminder
            }

            val notificationManagerDailyReminder: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerDailyReminder.createNotificationChannel(channelDailyReminder)


            //Steps Reminder
            val nameStepsReminder = Constants.CHANNEL_STEPS_REMINDER_TITLE
            val descriptionTextStepsReminder = Constants.CHANNEL_STEPS_REMINDER_DESCRIPTION
            val importanceStepsReminder = NotificationManager.IMPORTANCE_HIGH
            val channelStepsReminder = NotificationChannel(Constants.CHANNEL_STEPS_REMINDER_ID, nameStepsReminder, importanceStepsReminder).apply {
                description = descriptionTextStepsReminder
            }

            val notificationManagerStepsReminder: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerStepsReminder.createNotificationChannel(channelStepsReminder)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun scheduleDailyNotificationIfEnabled() {
            Log.d("SCHEDULE DAILY NOTIFICATION", "FROM MAIN ACTIVITY")

            val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
            val dailyReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)

            if (dailyReminderEnabled) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, DailyReminderReceiver::class.java)
                var pendingIntent = PendingIntent.getBroadcast(
                    this,
                    Constants.REQUEST_CODE_DAILY_REMINDER,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Check if PendingIntent already exists
                )

                if (pendingIntent == null) {
                    Log.d("NOTIFICATION", "pending intent null")
                    // PendingIntent non esiste, quindi crea un nuovo allarme
                    val hour = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, 8) // Default hour: 8
                    val minute = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, 0) // Default minute: 0

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }

                    pendingIntent = PendingIntent.getBroadcast(
                        this,
                        Constants.REQUEST_CODE_DAILY_REMINDER,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                } else {
                    Log.d("SCHEDULE DAILY NOTIFICATION", "Alarm already scheduled")
                }
            }


    }

    private fun scheduleStepsNotificationIfEnabled() {
        Log.d("SCHEDULE STEP NOTIFICATION", "FROM MAIN ACTIVITY")
        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)

        if (stepsReminderEnabled) {
            //val stepsNumber = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, 0) // Default days: 3

            // Calcola la data di trigger per la notifica
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, Constants.SHARED_PREFERENCES_STEPS_REMINDER_HOUR)
                set(Calendar.MINUTE, Constants.SHARED_PREFERENCES_STEPS_REMINDER_MINUTE)
                set(Calendar.SECOND, 0)
            }

            val intent = Intent(this, StepsReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                Constants.REQUEST_CODE_STEPS_REMINDER,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Check if PendingIntent already exists
            )

            if (pendingIntent == null) {
                Log.d("NOTIFICATION", "pending intent null")
                // PendingIntent non esiste, quindi crea un nuovo allarme
                val pendingIntentNew = PendingIntent.getBroadcast(
                    this,
                    Constants.REQUEST_CODE_STEPS_REMINDER,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntentNew
                )
            } else {
                Log.d("SCHEDULE STEP NOTIFICATION", "Alarm already scheduled")
            }
        }
    }



    private fun initializeNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_charts, R.id.navigation_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }


    private fun initializeObserverActivityTransition() {
        ActivityTransitionHandler.initialize(this, lifecycle)
    }





    // Handle result of permission request
    // Show dialog directing user to app settings for permission management
    fun showDialogSettingsActivityRecognition(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("You need permissions to register your physical activities!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", getPackageName(), null)
                intent.setData(uri)
                SETTINGS_PERMISSION_REQUEST.launch(intent)
                dialog.dismiss()


            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                this.finish()
            }
        builder.show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.PERMISSION_REQUESTS_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.size>0) {
                    var activityRec = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    var postNotification = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (!activityRec){
                        showDialogSettingsActivityRecognition()
                    }
                    if (activityRec && !postNotification){
                        showDialogSettingsNotification()
                    }
                }
            }
        }

    }

    fun showDialogSettingsNotification(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("You need permissions to send notifications!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", getPackageName(), null)
                intent.setData(uri)
                SETTINGS_PERMISSION_REQUEST.launch(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
                PermissionsHandler.notificationPermission = false
            }
        builder.show()
    }


    /*
    Todo: gestire la chiusura dell'app con attività in corso
     */
    override fun onDestroy() {
        super.onDestroy()
 /*
        // Gestione delle operazioni asincrone o persistenti
        lifecycleScope.launch {
            // Gestione della distruzione dell'attività principale
            //ActivityHandler.handleDestroy()

            // Gestione della terminazione di servizi o attività in background, se necessario
            val sharedPreferencesBackgroundActivities = this@MainActivity.getSharedPreferences(
                Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
            val backgroundRecognitionEnabled = sharedPreferencesBackgroundActivities.getBoolean(
                Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false)

            if (backgroundRecognitionEnabled) {
                ActivityTransitionHandler.handleOnDestroy()
            }

            // Altre operazioni di pulizia o persistenza
            // Assicurati che tutte le risorse siano liberate correttamente
        }

        Log.d("ON DESTROY", "Main activity onDestroy finished")

  */
    }

}





