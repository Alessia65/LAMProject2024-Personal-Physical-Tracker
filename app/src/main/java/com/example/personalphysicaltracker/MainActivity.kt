package com.example.personalphysicaltracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val activityRecognitionRequestCode: Int = 100
    private val notificationPermissionRequestCode: Int = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions if not granted
        requestPermissionsIfNeeded()

        // Initialize Sensors (example)
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

            //Daily Reminder
            val nameDailyReminder = "Daily Reminder Channel"
            val descriptionTextDailyReminder = "Channel for daily reminder notifications"
            val importanceDailyReminder = NotificationManager.IMPORTANCE_HIGH
            val channelDailyReminder = NotificationChannel("daily_reminder_channel", nameDailyReminder, importanceDailyReminder).apply {
                description = descriptionTextDailyReminder
            }

            val notificationManagerDailyReminder: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerDailyReminder.createNotificationChannel(channelDailyReminder)


            //Steps Reminder
            val nameStepsReminder = "Steps Reminder Channel"
            val descriptionTextStepsReminder = "Channel for steps reminder notifications"
            val importanceStepsReminder = NotificationManager.IMPORTANCE_HIGH
            val channelStepsReminder = NotificationChannel("steps_reminder_channel", nameStepsReminder, importanceStepsReminder).apply {
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
        val sharedPreferences = getSharedPreferences("settings daily reminder notification", Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferences.getBoolean("daily_reminder_enabled", false)
        if (dailyReminderEnabled) {
            val hour = sharedPreferences.getInt("daily_reminder_hour", 8) // Default hour: 8
            val minute = sharedPreferences.getInt("daily_reminder_minute", 0) // Default minute: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val intent = Intent(this, DailyReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
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
        val sharedPreferences = getSharedPreferences("settings minimum steps reminder notification", Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferences.getBoolean("steps_reminder_enabled", false)
        if (stepsReminderEnabled) {
            val stepsNumber = sharedPreferences.getInt("steps_reminder", 5000) // Default days: 3
            val hour = 17 // Orario desiderato per la notifica
            val minute = 0

            // Calcola la data di trigger per la notifica
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val intent = Intent(this, StepsReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                1,
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


    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isActivityRecognitionPermissionGranted()) {
                requestActivityRecognitionPermission()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isNotificationPermissionGranted()) {
                requestNotificationPermission()
            }
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                activityRecognitionRequestCode
            )
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                notificationPermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            activityRecognitionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION", "Activity recognition permission granted")
                }
            }
            notificationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION", "Notification permission granted")
                }
            }
        }
    }

    private fun isActivityRecognitionPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
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
}
