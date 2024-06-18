package com.example.personalphysicaltracker.ui.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentCalendarBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var calendarView: CalendarView
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayoutScrollView: LinearLayout

    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var activities:  List<PhysicalActivity>

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




        return root
    }

    private fun initializeViewModels() {
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        calendarViewModel.initializeActivityViewModel(this.activity, this)

    }



    private fun bindViews(root: View) {
        scrollView = root.findViewById(R.id.scroll_view)
        linearLayoutScrollView = root.findViewById(R.id.linear_layout_scroll)
        calendarView = root.findViewById(R.id.view_calendar)
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val selectedDate = calendar.time

            // Format date if needed
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate)

            // Handle selected date
            handleSelectedDate(formattedDate)
        }
    }

    private fun handleSelectedDate(formattedDate: String) {
        activities = calendarViewModel.obtainDates()

        // Filter activities based on selected date
        val activitiesForSelectedDate = activities.filter { (it.date) == formattedDate }
        Log.d("Date", formattedDate)
        // Log activities to console
        Log.d("Size", activitiesForSelectedDate.size.toString())
        addInScrollBar(activitiesForSelectedDate, false) //filters

    }

    private fun addInScrollBar(activitiesForSelectedDate: List<PhysicalActivity>, filters: Boolean) {
        binding.scrollView.removeAllViews()
        for (activity in activitiesForSelectedDate){
                createButtonOnScrollView(activity)
            }
        binding.scrollView.addView(linearLayoutScrollView)
    }

    private fun createButtonOnScrollView(activity: PhysicalActivity) {

        // Crea il nuovo pulsante
        val textView = TextView(requireContext())
        textView.text = "${activity.getActivityTypeName()}, ${activity.date}, ${activity.start}, ${activity.end}, ${activity.duration}"

        // Imposta i parametri di layout del pulsante (esempio)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textView.layoutParams = layoutParams

        // Aggiungi il pulsante alla ScrollView
        binding.linearLayoutScroll.addView(textView)

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}