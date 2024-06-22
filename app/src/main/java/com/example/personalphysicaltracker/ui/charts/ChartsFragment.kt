package com.example.personalphysicaltracker.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.databinding.FragmentChartsBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!
    private lateinit var chartsViewModel: ChartsViewModel

    private lateinit var textDate: TextView
    private lateinit var pieChart: PieChart
    private lateinit var buttonWalking: Button
    private lateinit var buttonDriving: Button
    private lateinit var buttonStanding: Button


    private lateinit var physicalActivities: List<PhysicalActivity>
    private var sumWalking = 0.0f
    private var sumDriving = 0.0f
    private var sumStanding = 0.0f
    private var sumsPieChart: Array<Float> = emptyArray()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chartsViewModel = ViewModelProvider(requireActivity()).get(ChartsViewModel::class.java)
        chartsViewModel.initializeActivityViewModel(this.activity, this)

        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeViews(root)
        return root
    }

    private fun initializeViews(root: View) {
        textDate = root.findViewById(R.id.text_date)
        // Ottieni la data corrente

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Imposta il testo della TextView alla data corrente
        textDate.text = currentDate
        textDate.setOnClickListener {
            showDateRangePicker()
        }

        pieChart = root.findViewById(R.id.pie_chart)
        setupPieChart()
        showTodayPieChart(currentDate)

        val navController = findNavController()

        buttonWalking = root.findViewById(R.id.button_walking)
        buttonWalking.setOnClickListener{
            navController.navigate(R.id.walkingChartsFragment)
        }

        buttonDriving = root.findViewById(R.id.button_driving)
        buttonDriving.setOnClickListener{
            navController.navigate(R.id.drivingChartsFragment)
        }

        buttonStanding = root.findViewById(R.id.button_standing)
        buttonStanding.setOnClickListener{
            navController.navigate(R.id.standingChartsFragment)
        }
        }


    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.isHighlightPerTapEnabled = false // Deprecated, use isHighlightPerTapEnabled

        // Listener per gestire il tap sul grafico
        pieChart.setOnChartGestureListener(object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
                // Non necessario implementare
            }

            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {
                // Non necessario implementare
            }

            override fun onChartLongPressed(me: MotionEvent?) {
                // Non necessario implementare
            }

            override fun onChartDoubleTapped(me: MotionEvent?) {
                // Non necessario implementare
            }

            override fun onChartSingleTapped(me: MotionEvent?) {
                // Mostra il dialog del PieChart
                chartsViewModel.saveDatesForDialog(sumsPieChart)
                val dialog = PieChartDialogFragment()
                dialog.show(parentFragmentManager, "PieChartDialogFragment")
            }

            override fun onChartFling(
                me1: MotionEvent?,
                me2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {
                // Non necessario implementare
            }

            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                // Non necessario implementare
            }

            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                // Non necessario implementare
            }
        })
    }


    private fun showTodayPieChart(currentDate: String) {
        val daysNumber = 1
        updateActivitiesInRange(currentDate, currentDate)
        sumDurations()
        populatePieChart(daysNumber)
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
                val formattedStartDate = chartsViewModel.convertTimeToDate(startDate)
                val formattedEndDate = chartsViewModel.convertTimeToDate(endDate)
                Log.d("DATE_RANGE_SELECTED", "$formattedStartDate - $formattedEndDate")
                val daysNumber = chartsViewModel.calculateRange(formattedStartDate, formattedEndDate)
                Log.d("NUMBER_OF_DAYS", "Number of days selected: $daysNumber")

                setTextDay(formattedStartDate,formattedEndDate)

                updateActivitiesInRange(formattedStartDate, formattedEndDate)
                sumDurations()
                populatePieChart(daysNumber)
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    private fun setTextDay(formattedStartDate: String, formattedEndDate: String) {
        if (formattedStartDate.equals(formattedEndDate)){
            textDate.text = formattedStartDate
        } else {
            textDate.text = formattedStartDate + " / " + formattedEndDate

        }
    }

    private fun updateActivitiesInRange(formattedStartDate: String, formattedEndDate: String) {
        physicalActivities = chartsViewModel.sendActivitiesInRange(formattedStartDate, formattedEndDate)
    }

    private fun sumDurations() {
        val sums = chartsViewModel.sumDurationOf(physicalActivities)
        sumWalking = sums[0].toFloat()
        sumDriving = sums[1].toFloat()
        sumStanding = sums[2].toFloat()
    }

    private fun populatePieChart(days: Int){
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry((sumWalking/3600), ""))
        entries.add(PieEntry((sumDriving/3600), ""))
        entries.add(PieEntry((sumStanding/3600), ""))


        var restOfTime = (24 * days) - (sumWalking/3600) - (sumDriving/3600) - (sumStanding/3600)
        entries.add(PieEntry(restOfTime, ""))

        sumsPieChart = arrayOf((sumWalking/3600), (sumDriving/3600), (sumStanding/3600), restOfTime)


        val dataSet = PieDataSet(entries, "Activities")
        dataSet.colors = listOf(Color.GREEN, Color.BLUE, Color.RED, Color.GRAY)
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.WHITE)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false

        pieChart.data = data
        pieChart.invalidate()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
