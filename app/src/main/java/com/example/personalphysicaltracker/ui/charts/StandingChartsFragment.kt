package com.example.personalphysicaltracker.ui.charts

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.databinding.FragmentChartsStandingBinding
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Date

class StandingChartsFragment : Fragment() {

    private var _binding: FragmentChartsStandingBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectDay: TextView
    private lateinit var selectWeek: TextView
    private lateinit var selectMonth: TextView
    private lateinit var selectYear: TextView
    private lateinit var textRange: TextView
    private lateinit var barChart: BarChart
    private var standingActivitiesToShow: List<StandingActivity> = emptyList()

    // ViewModel initialization
    private lateinit var chartViewModel: ChartsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsStandingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel
        chartViewModel = ViewModelProvider(requireActivity()).get(ChartsViewModel::class.java)

        initializeViews()
        showStarterChart()
        return root
    }

    private fun showStarterChart() {
        val startSelectedDate = Date()
        textRange.setText(chartViewModel.printDate(startSelectedDate, "DAY"))
        standingActivitiesToShow = chartViewModel.handleSelectedDateRange(ActivityType.STANDING) as List<StandingActivity>
        barChart = chartViewModel.showActivities(standingActivitiesToShow, barChart)
        barChart.invalidate()
    }

    // Initialize views and set click listeners
    private fun initializeViews() {
        textRange = binding.root.findViewById(R.id.text_range)
        selectDay = binding.root.findViewById(R.id.select_day_standing)
        selectDay.setOnClickListener {
            showDatePickerDialog("DAY")
        }
        selectWeek = binding.root.findViewById(R.id.select_week_standing)
        selectWeek.setOnClickListener {
            showDatePickerDialog("WEEK")
        }

        selectMonth = binding.root.findViewById(R.id.select_month_standing)
        selectMonth.setOnClickListener {
            showMonthPickerDialog()
        }

        selectYear = binding.root.findViewById(R.id.select_year_standing)
        selectYear.setOnClickListener {
            showYearDialog()
        }

        barChart = binding.root.findViewById(R.id.barChartStanding)
    }

    private fun showYearDialog() {
        val years = chartViewModel.getYearsToShow()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pick a year")
            .setItems(years) { dialog, item ->
                val selectedYear = years[item]

                textRange.text = selectedYear

                standingActivitiesToShow = chartViewModel.handleSelectedYear(selectedYear, ActivityType.STANDING) as List<StandingActivity>
                barChart = chartViewModel.showYearActivities(standingActivitiesToShow, barChart)
                barChart.invalidate()
            }

        val alert: AlertDialog = builder.create()
        alert.show()
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

                textRange.text = selectedMonth
                chartViewModel.setMonthDates(selectedMonth, selectedMonthPosition)
                standingActivitiesToShow = chartViewModel.handleSelectedMonth(ActivityType.STANDING) as List<StandingActivity>
                barChart = chartViewModel.showMonthActivities(standingActivitiesToShow, barChart)
                barChart.invalidate()
            }

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun showDatePickerDialog(type: String) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeCalendar)
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.show(requireActivity().supportFragmentManager, "DATE_PICKER")

        picker.addOnPositiveButtonClickListener {
            val startSelectedDate = Date(it)

            textRange.setText(chartViewModel.printDate(startSelectedDate, type))


            standingActivitiesToShow = chartViewModel.handleSelectedDateRange(ActivityType.STANDING) as List<StandingActivity>

            barChart = chartViewModel.showActivities(standingActivitiesToShow, barChart)
            barChart.invalidate()

        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
