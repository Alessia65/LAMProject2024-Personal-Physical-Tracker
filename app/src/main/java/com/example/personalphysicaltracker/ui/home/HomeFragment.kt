package com.example.personalphysicaltracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AlertDialog
import com.example.mvvm_todolist.TrackingRepository
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.Activity
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), AccelerometerListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var activityViewModel: ActivityViewModel

    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView

    private var selectedActivity: Activity? = null

    private var startTime: String = ""
    private var endTime: String = ""
    private var duration: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        val application = requireNotNull(this.activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)
        activityViewModel = ViewModelProvider(this, viewModelFactory).get(ActivityViewModel::class.java)

        textView = binding.textHome
        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        accelText = root.findViewById(R.id.acceleration_text)

        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }

        return root
    }

    private fun showActivitySelectionDialog() {
        val items = arrayOf("Walking", "Driving", "Standing")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Seleziona un'attività:")
            .setItems(items) { dialog, item ->
                when (items[item]) {
                    "Walking" -> startWalkingActivity()
                    "Driving" -> startDrivingActivity()
                    "Standing" -> startStandingActivity()
                }
            }
        val alert = builder.create()
        alert.show()
    }

    private fun startWalkingActivity() {
        selectedActivity = WalkingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor()
        startTime = getCurrentTime()
        changeButton()
    }

    private fun startDrivingActivity() {
        selectedActivity = DrivingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor()
        startTime = getCurrentTime()
        changeButton()
    }

    private fun startStandingActivity() {
        selectedActivity = StandingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor()
        startTime = getCurrentTime()
        changeButton()
    }

    private fun changeButton() {
        buttonStartActivity.text = "Stop Activity"
        buttonStartActivity.setOnClickListener {
            stopSelectedActivity()
        }
    }

    private fun stopSelectedActivity() {
        selectedActivity?.stopActivity()
        endTime = getCurrentTime()
        duration = calculateDuration(startTime, endTime)
        saveActivityData()

        buttonStartActivity.text = "Start Activity"
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }
        requireActivity().runOnUiThread {
            accelText.text = "No activity running"
        }
    }

    override fun onAccelerometerDataReceived(data: String) {
        requireActivity().runOnUiThread {
            accelText.text = data
        }
    }

    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun calculateDuration(start: String, end: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startTime = dateFormat.parse(start)?.time ?: 0L
        val endTime = dateFormat.parse(end)?.time ?: 0L
        return endTime - startTime
    }

    private fun saveActivityData() {
        val activityEntity = ActivityEntity(
            activityType = selectedActivity?.javaClass?.simpleName ?: "Unknown",
            dateStart = startTime,
            dateFinish = endTime,
            duration = duration
        )
        activityViewModel.insertActivityEntity(activityEntity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        selectedActivity?.stopActivity()
        _binding = null
    }
}
