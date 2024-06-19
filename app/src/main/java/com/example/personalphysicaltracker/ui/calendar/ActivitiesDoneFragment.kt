package com.example.personalphysicaltracker.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentActivitiesDoneBinding
import android.util.Log
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ActivitiesDoneFragment : Fragment() {

    private var _binding: FragmentActivitiesDoneBinding? = null
    private val binding get() = _binding!!
    private lateinit var activities: List<PhysicalActivity>
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayoutScrollView: LinearLayout
    private lateinit var chipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentActivitiesDoneBinding.inflate(inflater, container, false)

        initializeViews()
        initializeViewModels()
        obtainDates()




        return binding.root
    }

    private fun obtainDates() {
        activities = calendarViewModel.obtainActivitiesForTransaction()
        Log.d("ACTIVITIES OBTAINED", activities.size.toString())

        // Populate ScrollView with activities
        addInScrollBar()
    }

    private fun initializeViewModels() {
        // Initialize ViewModel using the Activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity()).get(CalendarViewModel::class.java)
    }


    // Initialize views from binding
    private fun initializeViews(){
        scrollView = binding.root.findViewById(R.id.scroll_view)
        linearLayoutScrollView = binding.root.findViewById(R.id.linear_layout_scroll)
        chipGroup = binding.root.findViewById(R.id.chip_group)
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Aggiungi il filtro selezionato
                    activities = calendarViewModel.addFilter(chip.text.toString())
                } else {
                    // Rimuovi il filtro selezionato
                    activities = calendarViewModel.removeFilter(chip.text.toString())
                }
                // Popola ScrollView con le attività filtrate
                addInScrollBar()
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view binding
        _binding = null
    }

    private fun addInScrollBar() {
        // Clear previous views in the ScrollView
        binding.scrollView.removeAllViews()
        binding.linearLayoutScroll.removeAllViews()

        if (activities.isNotEmpty()) {
            // Add each activity to the ScrollView
            for (activity in activities) {
                addInScrollView(activity)
            }

            // Add linearLayoutScrollView to the ScrollView
            binding.scrollView.addView(linearLayoutScrollView)
        }
    }

    private fun addInScrollView(activity: PhysicalActivity) {

        // Create a new TextView for each activity
        val textView = TextView(requireContext())
        textView.text = "${activity.getActivityTypeName()}, ${activity.date}, ${activity.start}, ${activity.end}, ${activity.duration}"

        // Set layout parameters for the TextView
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textView.layoutParams = layoutParams

        // Add the TextView to the LinearLayout within ScrollView
        binding.linearLayoutScroll.addView(textView)
    }
}
