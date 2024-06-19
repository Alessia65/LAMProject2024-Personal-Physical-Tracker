package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentCalendarBinding


class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarView: CalendarView
    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        initializeViewModels()

        // Bind views
        bindViews(root)

        return root
    }

    private fun initializeViewModels() {
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(this.activity, this)
    }

    private fun bindViews(root: View) {
        calendarView = root.findViewById(R.id.view_calendar)
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            changeFragment()
        }
    }

    private fun changeFragment() {
        // Naviga verso ActivitiesDoneFragment utilizzando NavController
        val navController = findNavController()
        navController.navigate(R.id.activitiesDoneFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
