package com.example.personalphysicaltracker.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentChartsWalkingBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkingChartsFragment : Fragment() {

    private var _binding: FragmentChartsWalkingBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectDay: TextView
    private lateinit var textRange: TextView
    private lateinit var barChart: BarChart
    private var walkingActivitiesToShow: List<WalkingActivity> = emptyList()

    // ViewModel initialization
    private lateinit var chartViewModel: ChartsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsWalkingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        chartViewModel = ViewModelProvider(requireActivity()).get(ChartsViewModel::class.java)

        initializeViews()
        return root
    }

    // Initialize views and set click listeners
    private fun initializeViews() {
        textRange = binding.root.findViewById(R.id.text_range)
        selectDay = binding.root.findViewById(R.id.select_day)
        selectDay.setOnClickListener {
            showDatePickerDialog("DAY")
        }

        barChart = binding.root.findViewById(R.id.barChart)
    }

    // Show date picker dialog based on type (DAY, WEEK, MONTH, YEAR)
    private fun showDatePickerDialog(type: String) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeCalendar)
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.show(requireActivity().supportFragmentManager, "DATE_PICKER")

        picker.addOnPositiveButtonClickListener {
            val startSelectedDate = Date(it)
            val startFormattedDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startSelectedDate)

            var endDate = ""

            if (type == "DAY") {
                endDate = startFormattedDate
                textRange.text = startFormattedDate
            } else if (type == "WEEK") {
                // endDate = 7 days after
            } else if (type == "MONTH") {
                // endDate = 30 days after
            } else if (type == "YEAR") {
                // endDate = 365 days after
            }

            if (startFormattedDate != null && endDate.isNotEmpty()) {
                walkingActivitiesToShow =
                    chartViewModel.handleSelectedDateRangeWalking(startFormattedDate, endDate)

                if (type == "DAY") {
                    showsDailyActivities()
                }
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    // Show daily activities in bar chart
    private fun showsDailyActivities() {
        val entries = ArrayList<BarEntry>()
        val sums = Array(24) { 0.0 }

        // Calculate sums per hour
        for (w in walkingActivitiesToShow) {
            val hourStart = w.start.substring(11, 13).toInt()
            val hourEnd = w.end.substring(11, 13).toInt()
            val durationInHour = w.duration / 3600.0

            for (i in hourStart until hourEnd) {
                sums[i] += durationInHour
            }
            Log.d("WA", "start: ${w.start}, end: ${w.end}, duration: ${w.duration}")
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        // Create BarDataSet and configure
        val barDataSet = BarDataSet(entries, "")
        barDataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.barBorderWidth = 0f
        barDataSet.setDrawValues(false) // Disable values above bars

        // Create BarData and configure
        val barData = BarData(barDataSet)
        barData.barWidth = 1.1f // Width of individual bars
        barData.isHighlightEnabled = false // Disable bar highlights

        // Configure BarChart
        barChart.setFitBars(true)
        barChart.data = barData
        barChart.description.text = "Hours of walking"

        // Animate BarChart
        barChart.animateY(2000)

        // Configure X axis
        barChart.xAxis.apply {
            setDrawAxisLine(false) // Disable X axis line
            setDrawGridLines(false) // Disable X axis grid lines
        }

        // Configure left Y axis
        barChart.axisLeft.apply {
            setDrawAxisLine(false) // Disable left Y axis line
            setDrawGridLines(false) // Disable left Y axis grid lines
        }

        // Configure right Y axis
        barChart.axisRight.apply {
            setDrawAxisLine(false) // Disable right Y axis line
            setDrawGridLines(false) // Disable right Y axis grid lines
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
