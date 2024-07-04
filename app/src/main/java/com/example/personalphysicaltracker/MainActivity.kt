package com.example.personalphysicaltracker

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalphysicaltracker.databinding.ActivityMainBinding
import com.example.personalphysicaltracker.handlers.AccelerometerSensorHandler
import com.example.personalphysicaltracker.handlers.StepCounterSensorHandler
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.NotificationHandler
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.ActivityHandlerViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activityHandlerViewModel: ActivityHandlerViewModel
    // ActivityResultLauncher to handle permission request result
    private val SETTINGS_PERMISSION_REQUEST = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
       handleSettingPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions if not already granted
        checkAndRequestPermissions()

        // Initialize navigation components
        initializeNavigation()

        activityHandlerViewModel = ViewModelProvider(this)[ActivityHandlerViewModel::class.java]
        initializeUtils()

        // Create notification channel
        createNotificationChannels()



    }

    private fun checkAndRequestPermissions() {
        if (!PermissionsHandler.checkPermissions(applicationContext)){
            PermissionsHandler.requestPermissions(this)
        } else {
            PermissionsHandler.notificationPermission = true
        }
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

    private fun initializeUtils() {
        val accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(this)
        val stepCounterSensorHandler = StepCounterSensorHandler.getInstance(this)
        ActivityTransitionHandler.initialize(this, lifecycle, this)
    }


    // Show dialog directing user to app settings for permission management
    private fun showDialogSettingsActivityRecognition(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("You need permissions to register your physical activities!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
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
                if (grantResults.isNotEmpty()) {
                    val activityRec = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val postNotification = grantResults[1] == PackageManager.PERMISSION_GRANTED

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

    private fun showDialogSettingsNotification(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("You need permissions to send notifications!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
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

    private fun handleSettingPermissionResult() {
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



    //TODO: non funziona bene
    override fun onDestroy() {
        CoroutineScope(Dispatchers.Main).launch {
            activityHandlerViewModel.handleDestroy(this@MainActivity)
        }
        super.onDestroy()
    }



}





