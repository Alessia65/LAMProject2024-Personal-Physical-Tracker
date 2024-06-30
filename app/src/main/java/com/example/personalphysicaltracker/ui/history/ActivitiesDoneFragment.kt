package com.example.personalphysicaltracker.ui.history

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentActivitiesDoneBinding
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.viewModels.CalendarViewModel
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
    private lateinit var calendarViewModel: CalendarViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityAdapter
    private lateinit var shareButton: ImageButton

    private lateinit var locationDetection: TextView
    private lateinit var chipGroup: ChipGroup



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activities_done, container, false)
        initializeViewModels()
        initializeViews(view)
        obtainDates()
        return view
    }


    private fun obtainDates() {
        activities = calendarViewModel.obtainActivitiesForTransaction()
        Log.d("ACTIVITIES OBTAINED", activities.size.toString())
        updateRecyclerView()

        // Populate ScrollView with activities
        //addInScrollBar()
    }

    private fun initializeViewModels() {
        // Initialize ViewModel using the Activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
    }


    // Initialize views from binding
    private fun initializeViews(view: View) {
        chipGroup = view.findViewById(R.id.chip_group)
        recyclerView = view.findViewById(R.id.recycler_view)
        shareButton = view.findViewById(R.id.button_share)
        locationDetection = view.findViewById(R.id.location_text)

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
            calendarViewModel.exportActivitiesToCSV(requireContext(), activities)
        }

        val sharedPreferencesBackgroundActivities = requireContext().getSharedPreferences(
            Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION,
            Context.MODE_PRIVATE
        )
        val enabled = sharedPreferencesBackgroundActivities.getBoolean(Constants.SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED, false)
        if (enabled){
            calendarViewModel.getTotalPresenceInLocation(requireContext())
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                getTotalPresenceInLocation()
            }
        } else {
            locationDetection.text = ""
        }

    }

    private fun getTotalPresenceInLocation() {
        var hours = (calendarViewModel.getDurationAtLocationInHours()).toString().substring(0,6)
        locationDetection.text = "You were detected in your interest's location for:\n" + hours + " hours"

    }


    private fun updateRecyclerView() {
        adapter.submitList(activities)
    }


    private val chipClickListener = View.OnClickListener { view ->
        if (view is Chip) {
            val checked = view.isChecked
            val activityType = view.text.toString()
            if (checked) {
                activities = calendarViewModel.addFilter(activityType)
            } else {
                activities = calendarViewModel.removeFilter(activityType)
            }
            updateRecyclerView()
        }
    }






    override fun onDestroyView() {
        super.onDestroyView()
        calendarViewModel.clearFilters()
        _binding = null
    }


}
