package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var calendarView: View

    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Initialize ViewModel
        initializeViewModels()

        // Bind views
        bindViews(root)

        //Load dates
        loadDates()

        initializeEvents()


        return root
    }

    private fun initializeViewModels() {
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(this.activity, this)

    }

    private fun loadDates() {

    }

    private fun initializeEvents() {
        calendarView.setOnClickListener(){

        }
    }

    private fun bindViews(root: View) {
        calendarView = root.findViewById(R.id.view_calendar)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}