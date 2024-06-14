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

        // Observer per monitorare i cambiamenti di dailyTime e aggiornare la UI
        homeViewModel.dailyTime.observe(viewLifecycleOwner) { dailyTimeList ->
            dailyTimeList?.let {
                val firstElement = it[0]
                val currentProgress = firstElement?.toInt() ?: 0
                updateProgressBar(currentProgress)
            }
        }

        textView = binding.textHome
        accelText = root.findViewById(R.id.acceleration_text)
        text_progress_bar = root.findViewById(R.id.text_progress_bar)
        progress_bar = root.findViewById<ProgressBar>(R.id.progress_bar)


        // Imposta il massimo della ProgressBar per rappresentare 24 ore in secondi
        val maxProgress = 24 * 3600 // 24 ore in secondi
        progress_bar.max = maxProgress


        initializeButtons(root)


        return root
    }

    private fun initializeButtons(root: ConstraintLayout){

        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }

    }

    private fun updateProgressBar(progressCurrent: Int){
        requireActivity().runOnUiThread {
            progress_bar.progress = progressCurrent
            text_progress_bar.text = progressBar.toString()
        }
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
        // Non è necessario ottenere di nuovo dailyTime qui, si può usare homeViewModel.dailyTime
        homeViewModel.dailyTime.value?.let { dailyTimeList ->
            val firstElement = dailyTimeList[0]
            val currentProgress = firstElement?.toInt() ?: 0
            updateProgressBar(currentProgress)
        }

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
