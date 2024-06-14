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
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding


class HomeFragment : Fragment(), AccelerometerListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var activityViewModel: ActivityViewModel

    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView



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
        homeViewModel.initializeModel(activityViewModel)

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
        changeButton()
    }

    private fun startWalkingActivity() {
        homeViewModel.startSelectedActivity(WalkingActivity(requireContext()), this)

    }

    private fun startDrivingActivity() {
        homeViewModel.startSelectedActivity(DrivingActivity(requireContext()), this)
    }

    private fun startStandingActivity() {
        homeViewModel.startSelectedActivity(StandingActivity(requireContext()), this)
    }

    private fun changeButton() {
        buttonStartActivity.text = "Stop Activity"
        buttonStartActivity.setOnClickListener {
            stopSelectedActivity()
        }
    }

    private fun stopSelectedActivity() {
        homeViewModel.stopSelectedActivity()


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







    override fun onDestroyView() {
        super.onDestroyView()
        homeViewModel.destoryActivity()
        _binding = null
    }
}
