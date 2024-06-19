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

class ActivitiesDoneFragment : Fragment() {

    private var _binding: FragmentActivitiesDoneBinding? = null
    private val binding get() = _binding!!
    private lateinit var activities: List<PhysicalActivity>
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayoutScrollView: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentActivitiesDoneBinding.inflate(inflater, container, false)

        // Initialize views from binding
        scrollView = binding.root.findViewById(R.id.scroll_view)
        linearLayoutScrollView = binding.root.findViewById(R.id.linear_layout_scroll)

        // Initialize ViewModel using the Activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity()).get(CalendarViewModel::class.java)

        // Retrieve activities saved in the ViewModel
        activities = calendarViewModel.obtainActivitiesForTransaction()
        Log.d("ACTIVITIES OBTAINED", activities.size.toString())

        // Populate ScrollView with activities
        addInScrollBar()

        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view binding
        _binding = null
    }

    private fun addInScrollBar() {
        // Clear previous views in the ScrollView
        binding.scrollView.removeAllViews()

        // Add each activity to the ScrollView
        for (activity in activities){
            addInScrollView(activity)
        }

        // Add linearLayoutScrollView to the ScrollView
        binding.scrollView.addView(linearLayoutScrollView)
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
