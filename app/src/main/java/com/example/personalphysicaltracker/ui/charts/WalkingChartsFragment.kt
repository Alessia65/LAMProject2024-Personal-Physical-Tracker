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
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentChartsWalkingBinding
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Date

class WalkingChartsFragment : Fragment() {

    private var _binding: FragmentChartsWalkingBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectDay: TextView
    private lateinit var selectWeek: TextView
    private lateinit var selectMonth: TextView
    private lateinit var selectYear: TextView
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
        selectDay = binding.root.findViewById(R.id.select_day_walking)
        selectDay.setOnClickListener {
            showDatePickerDialog("DAY")
        }
        selectWeek = binding.root.findViewById(R.id.select_week_walking)
        selectWeek.setOnClickListener {
            showDatePickerDialog("WEEK")
        }

        selectMonth = binding.root.findViewById(R.id.select_month_walking)
        selectMonth.setOnClickListener {
            showMonthPickerDialog()
        }

        selectYear = binding.root.findViewById(R.id.select_year_walking)
        selectYear.setOnClickListener {
            showYearDialog()
        }

        barChart = binding.root.findViewById(R.id.barChartWalking)
    }

    private fun showYearDialog() {
        val years = chartViewModel.getYearsToShow()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pick a year")
            .setItems(years) { dialog, item ->
                val selectedYear = years[item]

                textRange.text = selectedYear

                walkingActivitiesToShow = chartViewModel.handleSelectedYear(selectedYear, ActivityType.WALKING) as List<WalkingActivity>
                for (w in walkingActivitiesToShow){
                    Log.d("OK", w.date + ", " + w.getActivityTypeName().toString() + ", " + w.duration)
                }
                barChart = chartViewModel.showYearActivities(walkingActivitiesToShow, barChart)
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
                walkingActivitiesToShow = chartViewModel.handleSelectedMonth(ActivityType.WALKING) as List<WalkingActivity>
                barChart = chartViewModel.showMonthActivities(walkingActivitiesToShow, barChart)
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


            walkingActivitiesToShow = chartViewModel.handleSelectedDateRange(ActivityType.WALKING) as List<WalkingActivity>

            barChart = chartViewModel.showActivities(walkingActivitiesToShow, barChart)
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
