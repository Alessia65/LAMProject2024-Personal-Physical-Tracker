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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityHandler
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding
import com.example.personalphysicaltracker.ui.history.CalendarViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt


@SuppressLint("SetTextI18n")
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel


    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView
    private lateinit var progressBarWalking: ProgressBar
    private lateinit var progressBarDriving: ProgressBar
    private lateinit var progressBarStanding: ProgressBar
    private lateinit var walkingHours: TextView
    private lateinit var drivingHours: TextView
    private lateinit var standingHours: TextView
    private lateinit var dailyStepsText: TextView
    private lateinit var currentStepsText: TextView
    private lateinit var buttonSeeHistory: Button

    private var isWalkingActivity = false
    private var totalSteps= 0L

    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root



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

        if(ActivityHandler.stepCounterActive() || ActivityHandler.accelerometerActive()!=null){
            changeButtonToStop()
            accelText.text = ActivityHandler.accelerometerActive().toString() + " Activity Running"
        } else {
            accelText.text = "No activity running"
        }


        return root
    }

    // Initialize the ViewModel
    private fun initializeViewModel() {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        homeViewModel.initializeActivityHandler(this.activity, this, requireContext())

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
        dailyStepsText = root.findViewById(R.id.daily_steps_text)
        currentStepsText = root.findViewById(R.id.current_steps_text)
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
        ActivityHandler.dailyTime.observe(viewLifecycleOwner) { dailyTimeList ->
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

        ActivityHandler.actualSteps.observe(viewLifecycleOwner){ actualStepsList ->
            actualStepsList?.let {
                requireActivity().runOnUiThread {
                    homeViewModel.setSteps(totalSteps)
                    currentStepsText.text = "Current steps: $actualStepsList"
                    totalSteps = actualStepsList.toLong()
                }
            }
        }
        ActivityHandler.dailySteps.observe(viewLifecycleOwner){dailyStepsList ->
            dailyStepsList?.let{
                dailyStepsText.text = "Daily steps: $dailyStepsList"
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
            buttonStartActivity.isEnabled = false
            accelText.visibility = View.GONE
        } else {
            accelText.visibility = View.VISIBLE
        }

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
        //startAccelerometerSensor(ActivityType.WALKING)
        startStepCounterSensor()
        changeButtonToStop()
        isWalkingActivity = true
        accelText.text = "Walking Activity Running"
    }

    // Start the driving activity and change button to "Stop Activity"
    private fun startDrivingActivity() {
        homeViewModel.startSelectedActivity(DrivingActivity())
        //startAccelerometerSensor(ActivityType.DRIVING)
        changeButtonToStop()
        isWalkingActivity = false
        accelText.text = "Driving Activity Running"

    }

    // Start the standing activity and change button to "Stop Activity"
    private fun startStandingActivity() {
        homeViewModel.startSelectedActivity(StandingActivity())
        //startAccelerometerSensor(ActivityType.STANDING)
        changeButtonToStop()
        isWalkingActivity = false
        accelText.text = "Standing Activity Running"

    }

    private fun startStepCounterSensor(){
        val presenceStepCounterSensor = ActivityHandler.getStepCounterSensorHandler()
        if (!presenceStepCounterSensor){
            showAlert()
        }
    }



    //TODO: Far in modo che si deve per forza scegliere
    private fun showAlert() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Attention")
            .setMessage("Warning: Your device does not have a step counter sensor. The step count might be approximate. Do you still want to enable the step counter?")
            .setPositiveButton("Yes") { _, _ ->
                ActivityHandler.setStepCounterOnValue(true)
                ActivityHandler.setStepCounterWithAccValue(true)
            }
            .setNegativeButton("No") { _, _ ->
                ActivityHandler.setStepCounterOnValue(false)
                ActivityHandler.setStepCounterWithAccValue(false)
            }

        val alert = builder.create()
        alert.show()
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
        lifecycleScope.launch(Dispatchers.IO) {
            ActivityHandler.stopSelectedActivity(isWalkingActivity)
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