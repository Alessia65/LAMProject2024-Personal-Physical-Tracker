package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.personalphysicaltracker.databinding.FragmentActivitiesDoneBinding

class ActivitiesDoneFragment : Fragment() {

    private var _binding: FragmentActivitiesDoneBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentActivitiesDoneBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view binding
        _binding = null
    }
}



/*
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

     */

    /*
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val selectedDate = calendar.time

            // Format date if needed
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate)

            // Handle selected date
            handleSelectedDate(formattedDate)
        } */
