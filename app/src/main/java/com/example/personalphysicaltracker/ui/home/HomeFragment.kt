package com.example.personalphysicaltracker.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.sensors.AccelerometerListener
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding
import com.example.personalphysicaltracker.sensors.AccelerometerSensorHandler
import kotlin.math.roundToInt

class HomeFragment : Fragment(), AccelerometerListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var sensorHandler: AccelerometerSensorHandler

    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView
    private lateinit var progress_bar_walking: ProgressBar
    private lateinit var progress_bar_driving: ProgressBar
    private lateinit var progress_bar_standing: ProgressBar
    private lateinit var walking_hours: TextView
    private lateinit var driving_hours: TextView
    private lateinit var standing_hours: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        sensorHandler = AccelerometerSensorHandler(requireContext())
        // Initialize ViewModel
        initializeViewModel()

        // Bind views
        bindViews(root)


        // Set max values for progress bars
        setProgressBarMaxValues()

        // Observe LiveData changes
        observeDailyTimeChanges()

        // Initialize buttons
        initializeButtons(root as ConstraintLayout)

        return root
    }

    // Initialize the ViewModel
    private fun initializeViewModel() {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.initializeActivityViewModel(this.activity, this)
    }

    // Bind views to their respective IDs
    private fun bindViews(root: View) {
        accelText = root.findViewById(R.id.acceleration_text)
        progress_bar_walking = root.findViewById(R.id.progress_bar_walking)
        progress_bar_driving = root.findViewById(R.id.progress_bar_driving)
        progress_bar_standing = root.findViewById(R.id.progress_bar_standing)
        walking_hours = root.findViewById(R.id.text_walking_hours)
        driving_hours = root.findViewById(R.id.text_driving_hours)
        standing_hours = root.findViewById(R.id.text_standing_hours)
    }

    // Set the maximum value for progress bars to represent 24 hours in seconds
    private fun setProgressBarMaxValues() {
        val maxProgress = 24 * 3600 // 24 hours in seconds
        progress_bar_walking.max = maxProgress
        progress_bar_driving.max = maxProgress
        progress_bar_standing.max = maxProgress
    }



    // Observe changes in dailyTime LiveData and update UI accordingly
    private fun observeDailyTimeChanges() {
        homeViewModel.dailyTime.observe(viewLifecycleOwner) { dailyTimeList ->
            dailyTimeList?.let {
                val currentProgressWalking = dailyTimeList[0]?.toDouble() ?: 0.0
                val currentProgressDriving = dailyTimeList[1]?.toDouble() ?: 0.0
                val currentProgressStanding = dailyTimeList[2]?.toDouble() ?: 0.0
                Log.d("VALORI ARRIVATI", "$currentProgressWalking, $currentProgressDriving, $currentProgressStanding")
                updateProgressBarWalking(currentProgressWalking)
                updateProgressBarDriving(currentProgressDriving)
                updateProgressBarStanding(currentProgressStanding)
            }
        }
    }






    // Initialize buttons and set their click listeners
    private fun initializeButtons(root: ConstraintLayout) {
        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }
    }


    // Update the walking progress bar and hours
    private fun updateProgressBarWalking(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_walking.progress = progressCurrentInt
            walking_hours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
        }
    }

    // Update the driving progress bar and hours
    private fun updateProgressBarDriving(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_driving.progress = progressCurrentInt
            driving_hours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
        }
    }

    // Update the standing progress bar and hours
    private fun updateProgressBarStanding(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_standing.progress = progressCurrentInt
            standing_hours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
        }
    }



    // Show a dialog to select an activity
    private fun showActivitySelectionDialog() {
        val items = arrayOf("Walking", "Driving", "Standing")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Select an activity:")
            .setItems(items) { _, item ->
                when (items[item]) {
                    "Walking" -> startWalkingActivity()
                    "Driving" -> startDrivingActivity()
                    "Standing" -> startStandingActivity()
                }
            }
        builder.create().show()
    }

    // Start the walking activity and change button to "Stop Activity"
    private fun startWalkingActivity() {
        homeViewModel.startSelectedActivity(WalkingActivity())
        startSensor()
        changeButtonToStop()
    }

    // Start the driving activity and change button to "Stop Activity"
    private fun startDrivingActivity() {
        homeViewModel.startSelectedActivity(DrivingActivity())
        startSensor()
        changeButtonToStop()
    }

    // Start the standing activity and change button to "Stop Activity"
    private fun startStandingActivity() {
        homeViewModel.startSelectedActivity(StandingActivity())
        startSensor()
        changeButtonToStop()
    }

    fun startSensor(){
        sensorHandler.registerAccelerometerListener(this)
        sensorHandler.startAccelerometer()
    }

    fun stopSensor(){
        sensorHandler.unregisterListener()
        sensorHandler.stopAccelerometer()

    }

    // Change the button text and behavior to "Stop Activity"
    private fun changeButtonToStop() {
        buttonStartActivity.text = "Stop Activity"
        buttonStartActivity.setOnClickListener {
            stopSelectedActivity()
        }
    }

    // Stop the currently selected activity and reset the button to "Start Activity"
    private fun stopSelectedActivity() {
        stopSensor()
        homeViewModel.stopSelectedActivity()
        resetButtonToStart()
        requireActivity().runOnUiThread {
            accelText.text = "No activity running"
        }
    }

    // Reset the button text and behavior to "Start Activity"
    private fun resetButtonToStart() {
        buttonStartActivity.text = "Start Activity"
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }
    }


    // Handle accelerometer data updates
    override fun onAccelerometerDataReceived(data: String) {
        requireActivity().runOnUiThread {
            accelText.text = data
        }


        homeViewModel.dailyTime.value?.let { dailyTimeList ->
            val currentProgressWalking = dailyTimeList[0]?.toDouble() ?: 0.0
            val currentProgressDriving = dailyTimeList[1]?.toDouble() ?: 0.0
            val currentProgressStanding = dailyTimeList[2]?.toDouble() ?: 0.0
            Log.d("CHIUSURA ACTIVITY", "valori correnti: $currentProgressWalking, $currentProgressDriving, $currentProgressStanding")
            updateProgressBarWalking(currentProgressWalking)
            updateProgressBarDriving(currentProgressDriving)
            updateProgressBarStanding(currentProgressStanding)
        }


    }


    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }
    // Clean up bindings and stop the activity on view destruction
    override fun onDestroyView() {
        super.onDestroyView()
        stopSensor()
        _binding = null
    }
}
