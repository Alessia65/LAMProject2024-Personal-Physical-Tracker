package com.example.personalphysicaltracker

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.material.bottomnavigation.BottomNavigationView
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val activityRecognitionRequestCode: Int = 100

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions if not granted
        if (!isPermissionGranted()) {
            requestPermissions()
        }

        // Initialize Sensors -> DON'T DELETE
        val accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(this)
        val stepCounterSensorHandler = StepCounterSensorHandler.getInstance(this)

        // Initialize navigation components
        initializeNavigation()
    }

    // Handle click on Up button in ActionBar
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                activityRecognitionRequestCode
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            activityRecognitionRequestCode -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("PERMISSION", "Permission granted")
                }
            }
        }
    }

    private fun initializeNavigation() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Define top-level destinations for AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,  R.id.navigation_charts, R.id.navigation_calendar,
            )
        )

        // Setup ActionBar with NavController and AppBarConfiguration
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup BottomNavigationView with NavController
        navView.setupWithNavController(navController)
    }
}
