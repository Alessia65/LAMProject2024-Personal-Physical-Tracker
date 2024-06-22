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
        selectWeek.setOnClickListener{
            showDatePickerDialog("WEEK")
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
            val calendar = Calendar.getInstance()
            calendar.time = startSelectedDate

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startFormattedDate = sdf.format(startSelectedDate)

            var endDate = ""

            if (type == "DAY") {
                endDate = startFormattedDate
                textRange.text = startFormattedDate
            } else if (type == "WEEK") {
                calendar.add(Calendar.DAY_OF_MONTH, 6) // Aggiunge 6 giorni
                endDate = sdf.format(calendar.time)
                textRange.text = "$startFormattedDate / $endDate"
            } else if (type == "MONTH") {
                // endDate = 30 days after
            } else if (type == "YEAR") {
                // endDate = 365 days after
            }

            if (endDate.isNotEmpty()) {
                walkingActivitiesToShow =
                    chartViewModel.handleSelectedDateRangeWalking(startFormattedDate, endDate)

                for (w in walkingActivitiesToShow){
                    Log.d("VALUES IN RANGE", w.date + ", " + w.duration + ", " +  w.getActivityTypeName().toString())
                }
                if (type == "DAY") {
                    showsDailyActivities()
                } else if (type == "WEEK") {
                    // Reset calendar to start date for correct iteration
                    calendar.time = startSelectedDate
                    var referenceDates = Array(7) {
                        val currentDate = calendar.time
                        val formattedDate = sdf.format(currentDate)
                        calendar.add(Calendar.DAY_OF_MONTH, 1) // Aggiungi un giorno per la prossima iterazione
                        formattedDate // Restituisce la data formattata
                    }

                    for (date in referenceDates) {
                        Log.d("DATES", date)
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
                if (w.date == referenceDates[i]){
                    sums[i] += durationInHour
                }
            }

            Log.d("WA WEEK", "start: ${w.start}, end: ${w.end}, duration: ${w.duration}")

            // Create BarEntry objects
            for (i in sums.indices) {
                entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
            }

            val barData = configureBarData(entries)
            barChart.description.text = "Hours of walking"
            configureBarChart(barChart, barData, referenceDates)
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
            Log.d("WA DAILY", "start: ${w.start}, end: ${w.end}, duration: ${w.duration}")
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }
        val barData = configureBarData(entries)
        barChart.description.text = "Hours of walking"
        configureBarChart(barChart, barData, null)


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

    private fun configureBarChart(barChart: BarChart, barData: BarData, referenceDates: Array<String>?) {

        // Configure BarChart
        barChart.setFitBars(true)
        barChart.data = barData

        // Animate BarChart
        barChart.animateY(2000)

        // Configure X axis
        barChart.xAxis.apply {
            setDrawAxisLine(false) // Disable X axis line
            setDrawGridLines(false) // Disable X axis grid lines
            if (referenceDates!=null){
                var dates: Array<String> = Array(7) { "" }
                for (i in 0 until referenceDates.size){
                    dates[i] = referenceDates[i].substring(6)
                }
                valueFormatter = IndexAxisValueFormatter(dates ?: arrayOf()) // Set day names as X axis labels

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
