package com.example.personalphysicaltracker.ui.home

import android.os.Bundle
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
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding


class HomeFragment : Fragment(), AccelerometerListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button
    private lateinit var btn_decr: Button
    private lateinit var btn_incr: Button
    private lateinit var text_progress_bar: TextView
    private lateinit var accelText: TextView
    private lateinit var progress_bar: ProgressBar

    private var progressBar = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)


        homeViewModel.initializeModel(this.activity, this)

        textView = binding.textHome
        accelText = root.findViewById(R.id.acceleration_text)
        text_progress_bar = root.findViewById(R.id.text_progress_bar)
        progress_bar = root.findViewById<ProgressBar>(R.id.progress_bar)

        updateProgressBar()
        initializeButtons(root)


        return root
    }

    private fun initializeButtons(root: ConstraintLayout){
        btn_decr = root.findViewById(R.id.btn_decr)
        btn_incr = root.findViewById(R.id.btn_incr)

        btn_decr.setOnClickListener(){
            if (progressBar >= 5) {
                progressBar -= 5
                updateProgressBar()
            }
        }

        btn_incr.setOnClickListener(){
            if (progressBar <= 95) {
                progressBar += 5
                updateProgressBar()
            }
        }

        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }

    }

    private fun updateProgressBar(){
        progress_bar.progress = progressBar
        text_progress_bar.text = progressBar.toString()

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
