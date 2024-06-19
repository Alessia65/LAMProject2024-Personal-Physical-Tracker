package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentCalendarBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarView: CalendarView
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var activities: List<PhysicalActivity>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        initializeViewModels()

        // Bind views and set listeners
        bindViews(root)

        return root
    }

    /**
     * Initialize ViewModel and retrieve data from ViewModelStoreOwner (activity).
     */
    private fun initializeViewModels() {
        // Initialize ViewModel using the activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(requireActivity())
    }

    /**
     * Bind views and set CalendarView listener.
     */
    private fun bindViews(root: View) {
        calendarView = root.findViewById(R.id.view_calendar)
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val selectedDate = calendar.time

            // Format date if needed
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate)

            // Handle selected date and filter activities
            activities = handleSelectedDate(formattedDate)

            Log.d("ACTIVITIES FILTERED", activities.size.toString())

            // Navigate to ActivitiesDoneFragment
            changeFragment()
        }
    }

    /**
     * Handle selected date and obtain activities from ViewModel.
     */
    private fun handleSelectedDate(formattedDate: String): List<PhysicalActivity> {
        activities = calendarViewModel.obtainDates()
        Log.d("ACTIVITIES FROM DB", activities.size.toString())

        // Filter activities based on selected date
        return activities.filter { it.date == formattedDate }
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
}
