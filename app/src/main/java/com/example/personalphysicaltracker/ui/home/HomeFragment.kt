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
import kotlin.math.roundToInt


/*

TODO: modificare il db per tenere traccia del giorno di riferimento in realtà gia c'è valutare se inserire una nuova colonna oppure no
 */

class HomeFragment : Fragment(), AccelerometerListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView
    private lateinit var progress_bar_walking: ProgressBar
    private lateinit var progress_bar_driving: ProgressBar
    private lateinit var progress_bar_standing: ProgressBar
    private lateinit var walking_hours: TextView
    private lateinit var driving_hours: TextView
    private lateinit var standing_hours: TextView

    private var progressBarWalking = 0
    private var progressBarDriving = 0
    private var progressBarStanding = 0




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
        progress_bar_walking = root.findViewById<ProgressBar>(R.id.progress_bar_walking)
        progress_bar_driving = root.findViewById<ProgressBar>(R.id.progress_bar_driving)
        progress_bar_standing = root.findViewById<ProgressBar>(R.id.progress_bar_standing)

        walking_hours = root.findViewById(R.id.text_walking_hours)
        driving_hours = root.findViewById(R.id.text_driving_hours)
        standing_hours = root.findViewById(R.id.text_standing_hours)

        // Imposta il massimo della ProgressBar per rappresentare 24 ore in secondi
        val maxProgress = 24 * 3600 // 24 ore in secondi
        progress_bar_walking.max = maxProgress
        progress_bar_driving.max = maxProgress
        progress_bar_standing.max = maxProgress


        // Observer per monitorare i cambiamenti di dailyTime e aggiornare la UI
        homeViewModel.dailyTime.observe(viewLifecycleOwner) { dailyTimeList ->
            dailyTimeList?.let {
                val currentProgressWalking = dailyTimeList[0]?.toDouble() ?: 0.0
                val currentProgressDriving = dailyTimeList[1]?.toDouble() ?: 0.0
                val currentProgressStanding = dailyTimeList[2]?.toDouble() ?: 0.0
                updateProgressBarWalking(currentProgressWalking)
                updateProgressBarDriving(currentProgressDriving)
                updateProgressBarStanding(currentProgressStanding)
            }
        }



        initializeButtons(root)


        return root
    }

    private fun initializeButtons(root: ConstraintLayout){

        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }

    }

    private fun updateProgressBarWalking(progressCurrent: Double){
        var progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_walking.progress = progressCurrentInt
            walking_hours.text = (progressCurrent/3600).toString()
        }
    }

    private fun updateProgressBarDriving(progressCurrent: Double){
        var progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_driving.progress = progressCurrentInt
            driving_hours.text = (progressCurrent/3600).toString()
        }
    }

    private fun updateProgressBarStanding(progressCurrent: Double){
        var progressCurrentInt = progressCurrent.roundToInt()
        requireActivity().runOnUiThread {
            progress_bar_standing.progress = progressCurrentInt
            standing_hours.text = (progressCurrent/3600).toString()
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
            val currentProgressWalking = dailyTimeList[0]?.toDouble() ?: 0.0
            val currentProgressDriving = dailyTimeList[1]?.toDouble() ?: 0.0
            val currentProgressStanding = dailyTimeList[2]?.toDouble() ?: 0.0
            updateProgressBarWalking(currentProgressWalking)
            updateProgressBarDriving(currentProgressDriving)
            updateProgressBarStanding(currentProgressStanding)
        }

        requireActivity().runOnUiThread {
            accelText.text = data
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        homeViewModel.destroyActivity()
        _binding = null
    }
}
