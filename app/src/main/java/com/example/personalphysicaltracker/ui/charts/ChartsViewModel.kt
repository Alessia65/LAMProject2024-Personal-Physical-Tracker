package com.example.personalphysicaltracker.ui.charts

import android.graphics.Color
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChartsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel

    var activitiesOnDb: List<PhysicalActivity> = emptyList() //Da non modificare
    var sumsPieChart: Array<Float> = emptyArray()
    private lateinit var startDate: String
    private lateinit var startSelectedDate: Date
    private lateinit var endDate: String
    private lateinit var actualType: String
    private lateinit var calendar: Calendar
    private var numberMonth: Int = -1
    private var numberMonthString: String = ""
    private var days = 0
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)

        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityViewModel::class.java]
        updateFromDatabase()
    }

    fun convertTimeToDate(time: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = time
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(utc.time)
    }

    fun calculateRange(startDate: String, endDate: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateObj = dateFormat.parse(startDate)
        val endDateObj = dateFormat.parse(endDate)

        val diffInMillis = endDateObj.time - startDateObj.time
        val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

        // Add 1 because we want to include both start and end dates
        return diffInDays.toInt() + 1
    }

    private fun updateFromDatabase(): List<PhysicalActivity> {
        viewModelScope.launch(Dispatchers.IO) {
            activitiesOnDb = activityViewModel.getListOfPhysicalActivities()
            for (a in activitiesOnDb){
                Log.d("DB", a.getActivityTypeName().toString() + ", " + a.start + ", " + a.end + ", " + a.date + ", " + a.duration)
            }
        }
        return activitiesOnDb
    }

    fun sendActivitiesInRange(formattedStartDate: String, formattedEndDate: String): List<PhysicalActivity> {
        return activitiesOnDb.filter { activity ->
            activity.date >= formattedStartDate && activity.date <= formattedEndDate
        }
    }

    fun sumDurationOf(activities: List<PhysicalActivity>): Array<Double> {
        var sum: Array<Double> = arrayOf(0.0, 0.0, 0.0)
        for (a in activities) {
            when (a.getActivityTypeName()) {
                ActivityType.WALKING -> sum[0] += a.duration
                ActivityType.DRIVING -> sum[1] += a.duration
                ActivityType.STANDING -> sum[2] += a.duration
                else -> { /* handle other activity types if needed */ }
            }
        }
        return sum
    }

    fun saveDatesForDialog(sums: Array<Float>){
        sumsPieChart = sums

    }

    fun obtainDatesForDialog(): Array<Float>{
        return sumsPieChart
    }

    fun handleSelectedDateRange(activityType: ActivityType): List<PhysicalActivity> {
        // Filtra le attività in base all'intervallo di date selezionato
        val physicalActivities = activitiesOnDb.filter { activity ->
            activity.date >= startDate.toString() && activity.date <= endDate &&
                    activity.getActivityTypeName() == activityType
        }


        return physicalActivities
    }

    fun handleSelectedMonth(activityType: ActivityType): List<PhysicalActivity> {
        var month = if (numberMonth <= 9){
            "0" + numberMonth
        } else {
            numberMonth.toString()
        }

        val physicalActivities = activitiesOnDb.filter { activity ->
            activity.date.substring(5,7) == month &&
                    activity.getActivityTypeName() == activityType
        }

        return physicalActivities

    }

    fun printDate(startSelectedDate: Date, type: String): String{
        startDate = ""
        endDate = ""
        actualType = type
        this.startSelectedDate = startSelectedDate

        calendar = Calendar.getInstance()
        calendar.time = startSelectedDate

        val startFormattedDate = sdf.format(startSelectedDate)
        startDate = startFormattedDate

        if (type == "DAY") {
            endDate = startFormattedDate
            return startFormattedDate
        } else  { // if (type == "WEEK")
            calendar.add(Calendar.DAY_OF_MONTH, 6) // Add 6 days
            endDate = sdf.format(calendar.time)
            return "$startFormattedDate / $endDate"
        }

    }

    fun showActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart): BarChart {

        if (actualType == "DAY"){
            return showDailyActivities(activitiesToShow, barChart)
        } else{ // if (actualType == "WEEK")
            return showWeeklyActivities(activitiesToShow, barChart)
        }
    }

    private fun showDailyActivities(
        activitiesToShow: List<PhysicalActivity>,
        barChart: BarChart
    ): BarChart {
        val entries = ArrayList<BarEntry>()
        val sums = Array(24) { 0.0 }

        // Calculate sums per hour
        for (activities in activitiesToShow) {
            val hourStart = activities.start.substring(11, 13).toInt()
            val hourEnd = activities.end.substring(11, 13).toInt()
            val durationInHour = activities.duration / 3600.0

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

        return configureBarChart(barChart, barData, emptyArray(), false, false, false)

    }


    private fun showWeeklyActivities(
        activitiesToShow: List<PhysicalActivity>,
        barChart: BarChart
    ) : BarChart{
        var referenceDates = getWeeklyDates()

        val entries = ArrayList<BarEntry>()
        val sums = Array(7) { 0.0 }

        // Calculate sums per day
        for (activities in activitiesToShow) {
            val durationInHour = activities.duration / 3600.0
            for (i in 0 until referenceDates.size) {
                if (activities.date == referenceDates[i]) {
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

        return configureBarChart(barChart, barData, referenceDates, true, false, false)


    }

    private fun configureBarChart(
        barChart: BarChart,
        barData: BarData,
        referenceDates: Array<String>,
        isWeeklyChart: Boolean,
        isMonthChart: Boolean,
        isYearChart: Boolean
    ) : BarChart {

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
                    IndexAxisValueFormatter(dates ?: arrayOf())

            } else if (isYearChart){
                val months = Array(12) { (it+1).toString() }

                valueFormatter =
                    IndexAxisValueFormatter(months ?: arrayOf())

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

        return barChart
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

    private fun getWeeklyDates(): Array<String> {
        calendar.time = startSelectedDate
        var referenceDates = Array(7) {
            val currentDate = calendar.time
            val formattedDate = sdf.format(currentDate)
            calendar.add(Calendar.DAY_OF_MONTH, 1) // Add one day for next iteration
            formattedDate
        }
        return referenceDates
    }



    fun setMonthDates(month: String, numberSendMonth: Int) {
        // Get current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        days = 0
        when (numberSendMonth) {
            4, 6, 9, 11 -> days = 30 // 30 days: April, June, September, November
            2 -> days = if (isLeapYear(currentYear)) 29 else 28 // 28 or 29 days: February
            else -> days = 31 // 31 days: others
        }
        numberMonthString = ""

        if (numberSendMonth <= 9) {
            numberMonthString = "0" + numberSendMonth.toString()
        }
        this.numberMonth = numberSendMonth

    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun showMonthActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart): BarChart {
        val entries = ArrayList<BarEntry>()

        val sums = Array(days) { 0.0 }
        val referenceDates = Array(days) { i ->
            if (i <= 8) {
                "0" + (i + 1)
            } else (i + 1).toString()
        }

        // Calculate sums per day
        for (activities in activitiesToShow) {
            val durationInHour = activities.duration / 3600.0
            for (i in 0 until days) {
                if (activities.date.substring(5, 7) == numberMonthString && activities.date.substring(8) == i.toString()) {
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
        return configureBarChart(barChart, barData, referenceDates, false, true, false)
    }

    fun getYearsToShow(): Array<String> {
        val yearsToShow = (currentYear downTo currentYear - 9).map { it.toString() }.toTypedArray()
        return yearsToShow
    }




    fun handleSelectedYear(selectedYear: String, activityType: ActivityType): List<PhysicalActivity> {
        Log.d("PROVA", activitiesOnDb.get(0).date.substring(0,4) + ", " + selectedYear)

        val physicalActivities = activitiesOnDb.filter { activity ->
            activity.date.substring(0,4) == selectedYear &&
                    activity.getActivityTypeName() == activityType
        }
        return physicalActivities
    }

    fun showYearActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart): BarChart {
        val entries = ArrayList<BarEntry>()

        val sums = Array(12) { 0.0 }

        // Calculate sums per day
        for (activities in activitiesToShow) {
            val durationInHour = activities.duration / 3600.0
            for (i in 1 until 13) {
                var currentMonth = i.toString()
                if (i <= 9){
                    currentMonth = "0" + i
                }
                if (activities.date.substring(0, 4) == currentYear.toString() && activities.date.substring(5,7) == currentMonth) {
                    sums[i-1] += durationInHour
                }
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChart.description.text = "Month Hours in Year"
        return configureBarChart(barChart, barData, emptyArray(), false, false, true)
    }

    //TODO: caso in cui un'attività è a ridosso, la data viene inserita come data in cui finisce l'attività dovrei farla dividere giornalemnte
}