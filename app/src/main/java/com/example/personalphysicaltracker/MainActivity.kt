package com.example.personalphysicaltracker

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalphysicaltracker.databinding.ActivityMainBinding
import com.example.personalphysicaltracker.handlers.AccelerometerSensorHandler
import com.example.personalphysicaltracker.handlers.ActivityHandler
import com.example.personalphysicaltracker.handlers.StepCounterSensorHandler
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.NotificationHandler
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


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

        // Schedule notifications


        //NotificationHandler.scheduleDailyNotificationIfEnabled(this)
        //NotificationHandler.scheduleStepsNotificationIfEnabled(this)


        initializeObserverActivityTransition()


    }

    private fun createNotificationChannels() {

        NotificationHandler.createDailyReminderChannel(this)
        NotificationHandler.createStepsReminderChannel(this)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ActivityHandler.handleDestroy(this@MainActivity)
            } catch (e: Exception) {
                Log.e("CIAO", "Exception in onDestroy: ${e.message}", e)
            }
        }
        super.onDestroy()
    }





}





