package com.example.personalphysicaltracker.ui.charts

import android.app.AlertDialog
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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WalkingChartsFragment : Fragment() {

    private var _binding: FragmentChartsWalkingBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectDay: TextView
    private lateinit var selectWeek: TextView
    private lateinit var selectMonth: TextView
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
        selectWeek = binding.root.findViewById(R.id.select_week)
        selectWeek.setOnClickListener {
            showDatePickerDialog("WEEK")
        }

        selectMonth = binding.root.findViewById(R.id.select_month)
        selectMonth.setOnClickListener {
            showMonthPickerDialog()
        }

        barChart = binding.root.findViewById(R.id.barChart)
    }

    private fun showMonthPickerDialog() {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pick a month")
            .setItems(monthNames) { dialog, item ->
                val selectedMonth = monthNames[item]
                val selectedMonthPosition = item + 1 // Month number

                updateDataForSelectedMonth(selectedMonth, selectedMonthPosition)
            }

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    // Function to check if a year is a leap year
    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun updateDataForSelectedMonth(month: String, numberMonth: Int) {
        walkingActivitiesToShow = chartViewModel.handleSelectedMonthWalking(numberMonth)
        textRange.text = month

        // Get current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        var days = 0
        when (numberMonth) {
            4, 6, 9, 11 -> days = 30 // 30 days: April, June, September, November
            2 -> days = if (isLeapYear(currentYear)) 29 else 28 // 28 or 29 days: February
            else -> days = 31 // 31 days: others
        }
        var numberMonthString = ""
        if (numberMonth <= 9) {
            numberMonthString = "0" + numberMonth.toString()
        }

        showMonthActivities(days, numberMonthString)
    }


    private fun showMonthActivities(days: Int, numberMonth: String) {
        val entries = ArrayList<BarEntry>()

        val sums = Array(days) { 0.0 }
        val referenceDates = Array(days) { i ->
            if (i <= 8) {
                "0" + (i + 1)
            } else (i + 1).toString()
        }

        // Calculate sums per day
        for (w in walkingActivitiesToShow) {
            val durationInHour = w.duration / 3600.0
            for (i in 0 until days) {
                if (w.date.substring(5, 7) == numberMonth && w.date.substring(8) == i.toString()) {
                    sums[i] += durationInHour
                }
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChart.description.text = "Daily Hours in Month"
        configureBarChart(barChart, barData, referenceDates, false, true)

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
            val calendar = Calendar.getInstance()
            calendar.time = startSelectedDate

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startFormattedDate = sdf.format(startSelectedDate)

            var endDate = ""

            if (type == "DAY") {
                endDate = startFormattedDate
                textRange.text = startFormattedDate
            } else if (type == "WEEK") {
                calendar.add(Calendar.DAY_OF_MONTH, 6) // Add 6 days
                endDate = sdf.format(calendar.time)
                textRange.text = "$startFormattedDate / $endDate"
            }

            if (endDate.isNotEmpty()) {
                walkingActivitiesToShow =
                    chartViewModel.handleSelectedDateRangeWalking(startFormattedDate, endDate)

                if (type == "DAY") {
                    showsDailyActivities()
                } else if (type == "WEEK") {
                    // Reset calendar to start date for correct iteration
                    calendar.time = startSelectedDate
                    var referenceDates = Array(7) {
                        val currentDate = calendar.time
                        val formattedDate = sdf.format(currentDate)
                        calendar.add(Calendar.DAY_OF_MONTH, 1) // Add one day for next iteration
                        formattedDate
                    }

                    showsWeeklyActivities(referenceDates)
                }
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    private fun showsWeeklyActivities(referenceDates: Array<String>) {
        val entries = ArrayList<BarEntry>()
        val sums = Array(7) { 0.0 }

        // Calculate sums per day
        for (w in walkingActivitiesToShow) {
            val durationInHour = w.duration / 3600.0
            for (i in 0 until referenceDates.size) {
                if (w.date == referenceDates[i]) {
                    sums[i] += durationInHour
                }
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChart.description.text = "Daily Hours in Week"

        configureBarChart(barChart, barData, referenceDates, true, false)
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
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChart.description.text = "Daily Hours"

        configureBarChart(barChart, barData, emptyArray(), false, false)

    }

    private fun configureBarData(entries: ArrayList<BarEntry>): BarData {
        val barDataSet = BarDataSet(entries, "")
        barDataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.barBorderWidth = 0f
        barDataSet.setDrawValues(false) // Disable values above bars

        // Create BarData and configure
        val barData = BarData(barDataSet)
        barData.barWidth = 1.1f // Width of individual bars
        barData.isHighlightEnabled = false // Disable bar highlights

        return barData
    }

    private fun configureBarChart(
        barChart: BarChart,
        barData: BarData,
        referenceDates: Array<String>,
        isWeeklyChart: Boolean,
        isMonthChart: Boolean
    ) {

        // Configure BarChart
        barChart.setFitBars(true)
        barChart.data = barData

        // Animate BarChart
        barChart.animateY(2000)

        // Configure X axis
        barChart.xAxis.apply {
            setDrawAxisLine(false) // Disable X axis line
            setDrawGridLines(false) // Disable X axis grid lines
            if (isMonthChart || isWeeklyChart) {
                lateinit var dates: Array<String>
                if (isWeeklyChart) { // Weekly view
                    dates = Array(7) { "" }
                    for (i in 0 until referenceDates.size) {
                        dates[i] = referenceDates[i].substring(6)
                    }
                } else { // Monthly view
                    dates = Array(referenceDates.size) { "" }
                    for (i in 0 until referenceDates.size) {
                        dates[i] = referenceDates[i]
                    }
                }

                valueFormatter =
                    IndexAxisValueFormatter(dates ?: arrayOf()) // Set day names as X axis labels

            }
            position = XAxis.XAxisPosition.BOTTOM
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
