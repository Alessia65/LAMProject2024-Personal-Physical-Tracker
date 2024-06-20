package com.example.personalphysicaltracker.ui.graphics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.databinding.FragmentGraphicsBinding
import com.google.android.material.datepicker.MaterialDatePicker

class GraphicsFragment : Fragment() {

    private var _binding: FragmentGraphicsBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var graphicsViewModel: GraphicsViewModel


    private lateinit var textDate: TextView

    private lateinit var physicalActivities: List<PhysicalActivity>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        graphicsViewModel = ViewModelProvider(this).get(GraphicsViewModel::class.java)
        graphicsViewModel.initializeActivityViewModel(this.activity, this)


        _binding = FragmentGraphicsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeViews(root)
        return root
    }

    private fun initializeViews(root: View) {
        textDate = root.findViewById(R.id.text_date)
        textDate.setOnClickListener{
            showDateRangePicker()
        }
    }

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
                val formattedStartDate = graphicsViewModel.convertTimeToDate(startDate)
                val formattedEndDate = graphicsViewModel.convertTimeToDate(endDate)
                Log.d("DATE_RANGE_SELECTED", "$formattedStartDate - $formattedEndDate")
                val daysNumber = graphicsViewModel.calculateRange(formattedStartDate, formattedEndDate)
                Log.d("NUMBER_OF_DAYS", "Number of days selected: $daysNumber")
                updateActivitiesInRange(formattedStartDate, formattedEndDate)
            }
        }



        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    private fun updateActivitiesInRange(formattedStartDate: String, formattedEndDate: String) {
        physicalActivities = graphicsViewModel.sendActivitiesInRange(formattedStartDate, formattedEndDate)
        for (a in physicalActivities){
            Log.d("DB", a.getActivityTypeName().toString() + ", " + a.start + ", " + a.end + ", " + a.date + ", " + a.duration)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}