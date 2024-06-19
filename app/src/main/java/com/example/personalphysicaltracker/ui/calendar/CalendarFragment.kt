package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentCalendarBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    //private lateinit var calendarViewModel: CalendarViewModel
    //private lateinit var activities: List<PhysicalActivity>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root
        /*
        showDateRangePicker()

        // Initialize ViewModel
        initializeViewModels()
        */
        // Show date range picker dialog


        return root
    }

    /*
    /**
     * Initialize ViewModel and retrieve data from ViewModelStoreOwner (activity).
     */
    private fun initializeViewModels() {
        // Initialize ViewModel using the activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(requireActivity())
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

                // Handle selected date range
                activities = handleSelectedDateRange(formattedStartDate, formattedEndDate)

                Log.d("ACTIVITIES FILTERED", activities.size.toString())

                // Navigate to ActivitiesDoneFragment
                changeFragment()
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    /**
     * Convert timestamp to date string.
     */
    private fun convertTimeToDate(time: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = time
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return format.format(utc.time)
    }

    /**
     * Handle selected date range and obtain activities from ViewModel.
     */
    private fun handleSelectedDateRange(startDate: String, endDate: String): List<PhysicalActivity> {
        activities = calendarViewModel.obtainDates()
        Log.d("ACTIVITIES FROM DB", activities.size.toString())

        // Filter activities based on selected date range
        return activities.filter {
            it.date >= startDate && it.date <= endDate
        }
    }

    /**
     * Save filtered activities in ViewModel and navigate to ActivitiesDoneFragment.
     */
    private fun changeFragment() {
        calendarViewModel.saveActivitiesForTransaction(activities)
        Log.d("ACTIVITIES CALENDAR", activities.size.toString())

        // Navigate to ActivitiesDoneFragment using NavController
        val navController = findNavController()
        navController.navigate(R.id.activitiesDoneFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view binding
        _binding = null
    }
    */
}
