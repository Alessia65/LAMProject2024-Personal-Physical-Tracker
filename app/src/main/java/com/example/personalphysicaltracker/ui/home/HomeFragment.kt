package com.example.personalphysicaltracker.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding
import com.example.personalphysicaltracker.sensors.AccelerometerListener
import com.example.personalphysicaltracker.sensors.AccelerometerSensorHandler
import com.example.personalphysicaltracker.sensors.StepCounterListener
import com.example.personalphysicaltracker.sensors.StepCounterSensorHandler
import com.example.personalphysicaltracker.ui.history.CalendarViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt


@SuppressLint("SetTextI18n")
class HomeFragment : Fragment(), AccelerometerListener, StepCounterListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var accelerometerSensorHandler: AccelerometerSensorHandler
    private lateinit var stepCounterSensorHandler: StepCounterSensorHandler

    private lateinit var buttonStartActivity: Button
    public lateinit var accelText: TextView
    private lateinit var progressBarWalking: ProgressBar
    private lateinit var progressBarDriving: ProgressBar
    private lateinit var progressBarStanding: ProgressBar
    private lateinit var walkingHours: TextView
    private lateinit var drivingHours: TextView
    private lateinit var standingHours: TextView
    private lateinit var stepsText: TextView
    private lateinit var buttonSeeHistory: Button

    private var stepCounterOn: Boolean = false
    private var isWalkingActivity = false
    private var totalSteps= 0L
    private var stepCounterWithAcc = false

    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        accelerometerSensorHandler = AccelerometerSensorHandler.getInstance(requireContext())
        stepCounterSensorHandler = StepCounterSensorHandler.getInstance(requireContext())

        initializeViewModel()

        // Bind views
        bindViews(root)

        // Initialize ViewModel


        // Set max values for progress bars
        setProgressBarMaxValues()

        // Observe LiveData changes
        observeDailyTimeChanges()
        observeDailyStepsChanges()

        // Initialize buttons
        initializeButtons(root)

        if(stepCounterSensorHandler.isActive() || accelerometerSensorHandler.isActive()!=null){
            changeButtonToStop()
            accelText.text = accelerometerSensorHandler.isActive().toString() + " Activity Running"
        } else {
            accelText.text = "No activity running"
        }


        return root
    }

    // Initialize the ViewModel
    private fun initializeViewModel() {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        homeViewModel.initializeActivityViewModel(this.activity, this)

        calendarViewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(requireActivity())
    }

    // Bind views to their respective IDs
    private fun bindViews(root: View) {
        accelText = root.findViewById(R.id.acceleration_text)
        progressBarWalking = root.findViewById(R.id.progress_bar_walking)
        progressBarDriving = root.findViewById(R.id.progress_bar_driving)
        progressBarStanding = root.findViewById(R.id.progress_bar_standing)
        walkingHours = root.findViewById(R.id.text_walking_hours)
        drivingHours = root.findViewById(R.id.text_driving_hours)
        standingHours = root.findViewById(R.id.text_standing_hours)
        stepsText = root.findViewById(R.id.text_steps)
    }

    // Set the maximum value for progress bars to represent 24 hours in seconds
    private fun setProgressBarMaxValues() {
        val maxProgress = 24 * 3600 // 24 hours in seconds
        progressBarWalking.max = maxProgress
        progressBarDriving.max = maxProgress
        progressBarStanding.max = maxProgress
    }



    // Observe changes in dailyTime LiveData and update UI accordingly
    private fun observeDailyTimeChanges() {
        homeViewModel.dailyTime.observe(viewLifecycleOwner) { dailyTimeList ->
            dailyTimeList?.let {
                val currentProgressWalking = dailyTimeList[0] ?: 0.0
                val currentProgressDriving = dailyTimeList[1] ?: 0.0
                val currentProgressStanding = dailyTimeList[2] ?: 0.0
                updateProgressBarWalking(currentProgressWalking)
                updateProgressBarDriving(currentProgressDriving)
                updateProgressBarStanding(currentProgressStanding)
            }
        }
    }

    
    private fun observeDailyStepsChanges(){
        homeViewModel.dailySteps.observe(viewLifecycleOwner){dailyStepsList ->
            dailyStepsList?.let{
                stepsText.text = "Daily steps: $dailyStepsList"
                val sharedPreferencesSteps = requireContext().getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
                val editor = sharedPreferencesSteps.edit()
                editor.putLong(Constants.SHARED_PREFERENCES_STEPS_DAILY, dailyStepsList)
                editor.apply()

            }
        }
    }



    // Initialize buttons and set their click listeners
    private fun initializeButtons(root: ConstraintLayout) {
        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }
        buttonSeeHistory = root.findViewById(R.id.button_history)
        buttonSeeHistory.setOnClickListener {
            showDateRangePicker()
        }

        val sharedPreferencesBackgroundActivities = requireContext().getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION, Context.MODE_PRIVATE)
        val  backgroundRecognitionEnabled = sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED, false)
        if (backgroundRecognitionEnabled){
            Log.d("CIAO","BRO")
            buttonStartActivity.isEnabled = false
        }
        Log.d("CIAO",backgroundRecognitionEnabled.toString())

    }


    // Update the walking progress bar and hours
    
    private fun updateProgressBarWalking(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progressBarWalking.progress = progressCurrentInt
            walkingHours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
        }
    }

    // Update the driving progress bar and hours
    
    private fun updateProgressBarDriving(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progressBarDriving.progress = progressCurrentInt
            drivingHours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
        }
    }

    // Update the standing progress bar and hours
    
    private fun updateProgressBarStanding(progressCurrent: Double) {
        val progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progressBarStanding.progress = progressCurrentInt
            standingHours.text = String.format("%.4f", progressCurrent / 3600.0) + "h"
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
        startAccelerometerSensor(ActivityType.WALKING)
        startStepCounterSensor()
        changeButtonToStop()
        isWalkingActivity = true
        accelText.text = "Walking Activity Running"
    }

    // Start the driving activity and change button to "Stop Activity"
    private fun startDrivingActivity() {
        homeViewModel.startSelectedActivity(DrivingActivity())
        startAccelerometerSensor(ActivityType.DRIVING)
        changeButtonToStop()
        isWalkingActivity = false
        accelText.text = "Driving Activity Running"

    }

    // Start the standing activity and change button to "Stop Activity"
    private fun startStandingActivity() {
        homeViewModel.startSelectedActivity(StandingActivity())
        startAccelerometerSensor(ActivityType.STANDING)
        changeButtonToStop()
        isWalkingActivity = false
        accelText.text = "Standing Activity Running"

    }

    private fun startAccelerometerSensor(activityType: ActivityType){
        accelerometerSensorHandler.registerAccelerometerListener(this)
        accelerometerSensorHandler.startAccelerometer(activityType)
    }

    private fun startStepCounterSensor(){
        stepCounterSensorHandler.registerStepCounterListener(this)
        val presenceStepCounterSensor = stepCounterSensorHandler.startStepCounter()
        if (!presenceStepCounterSensor){
            showAlert()
        } else{
            stepCounterOn = true
        }
    }



    private fun showAlert() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Attention")
            .setMessage("Warning: Your device does not have a step counter sensor. The step count might be approximate. Do you still want to enable the step counter?")
            .setPositiveButton("Yes") { _, _ ->
                stepCounterOn = true
                stepCounterWithAcc = true
            }
            .setNegativeButton("No") { _, _ ->
                stepCounterOn = false
                stepCounterWithAcc = false
            }

        val alert = builder.create()
        alert.show()
    }
    private fun stopAccelerometerSensor(){
        accelerometerSensorHandler.unregisterListener()
        accelerometerSensorHandler.stopAccelerometer()

    }

    private fun stopStepCounterSensor(){
        if (stepCounterOn) {
            stepCounterSensorHandler.unregisterListener()
            stepCounterSensorHandler.stopStepCounter()
            stepCounterOn = false
        }
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
        stopAccelerometerSensor()
        stopStepCounterSensor()
        if (isWalkingActivity){
            homeViewModel.stopSelectedActivity(totalSteps, true)
        } else {
            homeViewModel.stopSelectedActivity(0, false)

        }
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

        /*
        requireActivity().runOnUiThread {
            accelText.text = data
        }

         */

        if (stepCounterWithAcc) {
            registerStep(data)
        }
        homeViewModel.dailyTime.value?.let { dailyTimeList ->
            val currentProgressWalking = dailyTimeList[0] ?: 0.0
            val currentProgressDriving = dailyTimeList[1] ?: 0.0
            val currentProgressStanding = dailyTimeList[2] ?: 0.0
            updateProgressBarWalking(currentProgressWalking)
            updateProgressBarDriving(currentProgressDriving)
            updateProgressBarStanding(currentProgressStanding)
        }
    }


    private fun registerStep(data: String) {
        val step = stepCounterSensorHandler.registerStepWithAccelerometer(data)
        onStepCounterDataReceived(step.toString())
    }



    override fun onStepCounterDataReceived(data: String) {
        requireActivity().runOnUiThread {
            stepsText.text = "Current steps: $data"
            totalSteps = data.toLong()

        }
    }



    /**
     * Show the Material Date Range Picker dialog.
     */
    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTheme(R.style.ThemeCalendar)
            .setTitleText("Select range")
            .build()

        picker.show(requireActivity().supportFragmentManager, "DATE_RANGE_PICKER")

        picker.addOnPositiveButtonClickListener {
            val startDate = it.first
            val endDate = it.second
            if (startDate != null && endDate != null) {
                val formattedStartDate = convertTimeToDate(startDate)
                val formattedEndDate = convertTimeToDate(endDate)
                Log.d("DATE_RANGE_SELECTED", "$formattedStartDate - $formattedEndDate")

                calendarViewModel.handleSelectedDateRange(formattedStartDate, formattedEndDate)

                // Navigate to ActivitiesDoneFragment
                changeFragment()
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    private fun convertTimeToDate(time: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = time
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(utc.time)
    }



    private fun changeFragment() {
        //calendarViewModel.saveActivitiesForTransaction(activities)

        // Navigate to ActivitiesDoneFragment using NavController
        val navController = findNavController()
        navController.navigate(R.id.activitiesDoneFragment)
    }

    // Clean up bindings and stop the activity on view destruction
    override fun onDestroyView() {
        super.onDestroyView()
        //stopAccelerometerSensor()
        _binding = null
    }




}