package com.example.personalphysicaltracker

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.databinding.ActivityMainBinding
import com.example.personalphysicaltracker.handlers.AccelerometerSensorHandler
import com.example.personalphysicaltracker.handlers.StepCounterSensorHandler
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.NotificationHandler
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.ActivityDBViewModel
import com.example.personalphysicaltracker.viewModels.ActivityHandlerViewModel
import com.example.personalphysicaltracker.viewModels.ActivityViewModelFactory
import com.example.personalphysicaltracker.viewModels.ChartsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        if (getFirstOpen()){
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            navController.navigate(R.id.helpFragment)
            unsetFirstOpen()
        }

        checkIntegrityOfLocationDB()
    }

    private fun checkIntegrityOfLocationDB() {
        val activityViewModel = ViewModelProvider(this, ActivityViewModelFactory(TrackingRepository(application)))[ActivityDBViewModel::class.java]

        lifecycleScope.launch {
            val ids = withContext(Dispatchers.IO) {
                activityViewModel.getDuplicateIds()
            }
            delay(3000)
            if (ids.isNotEmpty()) {
                activityViewModel.deleteByIds(ids)
            }
        }
    }

    private fun getFirstOpen(): Boolean {
        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FIRST_OPEN, Context.MODE_PRIVATE)
        return  !(sharedPreferences.contains(Constants.FIRST_OPEN))
    }

    private fun unsetFirstOpen(){
        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FIRST_OPEN, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.FIRST_OPEN, false)
        editor.apply()
    }


    private fun checkAndRequestPermissions() {
        if (!PermissionsHandler.checkPermissionsActivityRecognition(applicationContext)){
            PermissionsHandler.requestPermissionsActivity(this)
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
        ActivityTransitionHandler.initialize(this, this)
        val chartsViewModel = ViewModelProvider(this)[ChartsViewModel::class.java]
        chartsViewModel.initializeActivityViewModel(this, this) //to obtain activities in chart

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
            Constants.PERMISSION_REQUESTS_CODE_ACTIVITY -> {
                if (grantResults.isNotEmpty()) {
                    val activityRec = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (!activityRec) {
                        showDialogSettingsActivityRecognition()
                    }
                }
            }
        }
    }


    private fun handleSettingPermissionResult() {
        // Check if activity recognition permission is granted
        if (!PermissionsHandler.checkPermissionsActivityRecognition(this)) {
            // Show settings dialog if permission is still not granted
            showDialogSettingsActivityRecognition()
        }
    }



    override fun onDestroy() {
        CoroutineScope(Dispatchers.Main).launch {
            activityHandlerViewModel.handleDestroy(this@MainActivity)
        }
        super.onDestroy()
    }



}





