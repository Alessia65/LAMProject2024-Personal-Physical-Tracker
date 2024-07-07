package com.example.personalphysicaltracker.ui.history

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentActivitiesDoneBinding
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.HistoryViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivitiesDoneFragment : Fragment() {

    private var _binding: FragmentActivitiesDoneBinding? = null
    private val binding get() = _binding!!

    private lateinit var activities: List<PhysicalActivity>
    private lateinit var historyViewModel: HistoryViewModel

    private lateinit var adapter: ActivityAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var shareButton: ImageButton
    private lateinit var locationDetection: TextView
    private lateinit var chipGroup: ChipGroup



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesDoneBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeViewModels()
        initializeViews(root)
        obtainDates()
        return root
    }

    private fun initializeViewModels() {
        historyViewModel = ViewModelProvider(requireActivity())[HistoryViewModel::class.java]
    }

    private fun initializeViews(root: View) {
        chipGroup = root.findViewById(R.id.chip_group)
        recyclerView = root.findViewById(R.id.recycler_view)
        shareButton = root.findViewById(R.id.button_share)
        locationDetection = root.findViewById(R.id.location_text)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivityAdapter()
        recyclerView.adapter = adapter

        // Set chip click listeners
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener(chipClickListener)
        }

        shareButton.setOnClickListener{
            historyViewModel.exportActivitiesToCSV(requireContext(), activities)
        }

        val sharedPreferencesBackgroundActivities = requireContext().getSharedPreferences(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION, Context.MODE_PRIVATE)
        val enabled = sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false)

        if (enabled){
            historyViewModel.getTotalPresenceInLocation(requireContext())
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                getTotalPresenceInLocation()
            }
        } else {
            locationDetection.text = ""
        }
    }


    private fun obtainDates() {
        activities = historyViewModel.obtainActivitiesForTransaction()
        updateRecyclerView()
    }

    private fun getTotalPresenceInLocation() {
        val duration = (historyViewModel.getDurationAtLocationInHours())
        var text = "0.0"
        if (duration!=0.0){
            text = if (duration.toString().length>=6) {
                duration.toString().substring(0, 6)
            } else {
                duration.toString()
            }
        }
        text = "You were detected in your interest's location for:\n$text hours"
        locationDetection.text = text

    }


    private fun updateRecyclerView() {
        adapter.submitList(activities)
    }


    private val chipClickListener = View.OnClickListener { view ->
        if (view is Chip) {
            val checked = view.isChecked
            val activityType = view.text.toString()
            activities = if (checked) {
                historyViewModel.addFilter(activityType)
            } else {
                historyViewModel.removeFilter(activityType)
            }
            updateRecyclerView()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        historyViewModel.clearFilters()
        _binding = null
    }


}
