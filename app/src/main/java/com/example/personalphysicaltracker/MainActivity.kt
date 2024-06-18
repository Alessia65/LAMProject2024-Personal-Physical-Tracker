package com.example.personalphysicaltracker

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding

    private val ACTIVITY_RECOGNITION_REQUEST_CODE: Int = 100


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isPermissionGranted()){
            requestPermissions()
        }
        //Sensori
        var accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(this)
        var stepCounterSensorHandler = StepCounterSensorHandler.getInstance(this)

        initializeNavigation()

    }

    private fun requestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), ACTIVITY_RECOGNITION_REQUEST_CODE)
            Log.d("API","Non sei scarso")
        } else {
            Log.d("API","sei scarso")
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    //Permission grant
                }
            }
        }
    }


    private fun initializeNavigation(){
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )


        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}