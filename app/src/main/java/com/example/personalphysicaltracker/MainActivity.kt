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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalphysicaltracker.databinding.ActivityMainBinding
import com.example.personalphysicaltracker.sensors.AccelerometerSensorHandler
import com.example.personalphysicaltracker.sensors.StepCounterSensorHandler
import com.example.personalphysicaltracker.notifications.DailyReminderReceiver
import com.example.personalphysicaltracker.notifications.StepsReminderReceiver
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.net.URI
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ActivityResultLauncher to handle permission request result
    private val SETTINGS_PERMISSION_REQUEST = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Check if activity recognition permission is granted
        if (ActivityCompat.checkSelfPermission(this, Constants.PERMISSION_ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
        } else {
            // Show settings dialog if permission is still not granted
            showDialogSettings()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request activity recognition permission if not granted
        requestActivityRecnognisePermission()

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
    }



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
        Log.d("SCHEDULE DAILY NOTIFICATION","FROM MAIN ACTIVITY")

        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)
        if (dailyReminderEnabled) {
            val hour = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, 8) // Default hour: 8
            val minute = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, 0) // Default minute: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val intent = Intent(this, DailyReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                Constants.REQUEST_CODE_DAILY_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun scheduleStepsNotificationIfEnabled() {
        Log.d("SCHEDULE STEP NOTIFICATION","FROM MAIN ACTIVITY")
        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)
        if (stepsReminderEnabled) {
            val stepsNumber = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_STEPS_REMINDER_NUMBER, 0) // Default days: 3


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
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
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

    // Request activity recognition permission if not granted
    fun requestActivityRecnognisePermission() {
        if (PermissionsHandler.checkActivityRecognitionPermission(this)) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
        } else if (PermissionsHandler.shouldShowRequest(this)) {
            showDialogRequest()
        } else {
            PermissionsHandler.requestActivityRecognitionPermission(this)
        }
    }

    // Show dialog requesting activity recognition permission
    fun showDialogRequest(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This app requires ACTIVITY RECOGNITION permission to register your physical activities")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                PermissionsHandler.requestActivityRecognitionPermission(this)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                this.finish()
            }
        builder.show()
    }

    // Handle result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (requestCode == Constants.ACTIVITY_RECOGNITION_REQUEST_CODE){
            if (PermissionsHandler.checkGrantResults(grantResults)) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
            } else if (!PermissionsHandler.shouldShowRequest(this)){
                showDialogSettings()
            }
        }
    }

    // Show dialog directing user to app settings for permission management
    fun showDialogSettings(){
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


}
