package com.example.personalphysicaltracker.viewModels

import android.graphics.Color
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChartsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityDBViewModel

    private var activitiesOnDb: List<PhysicalActivity> = emptyList()
    private var sumsPieChart: Array<Float> = emptyArray()
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
    private lateinit var activityType: ActivityType

    fun initializeActivityViewModel(activity: FragmentActivity?, viewModelStoreOwner: ViewModelStoreOwner) {
        val application = requireNotNull(activity).application
        val repository = TrackingRepository(application)
        val viewModelFactory = ActivityViewModelFactory(repository)

        activityViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory)[ActivityDBViewModel::class.java]
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
        var diffInMillis = 0L
        if (startDateObj != null && endDateObj!= null) {
            diffInMillis = endDateObj.time - startDateObj.time
        }
        val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

        // Add 1 to include both start and end dates
        return diffInDays.toInt() + 1
    }

    private fun updateFromDatabase(): List<PhysicalActivity> {
        viewModelScope.launch(Dispatchers.IO) {
            activitiesOnDb = activityViewModel.getListOfPhysicalActivities()
        }
        return activitiesOnDb
    }

    fun sendActivitiesInRange(formattedStartDate: String, formattedEndDate: String): List<PhysicalActivity> {
        return activitiesOnDb.filter { activity ->
            activity.date in formattedStartDate..formattedEndDate
        }
    }

    fun sumDurationOf(activities: List<PhysicalActivity>): Array<Double> {
        val sum: Array<Double> = arrayOf(0.0, 0.0, 0.0)
        for (a in activities) {
            when (a.getActivityTypeName()) {
                ActivityType.WALKING -> sum[0] += a.duration
                ActivityType.DRIVING -> sum[1] += a.duration
                ActivityType.STANDING -> sum[2] += a.duration
                else -> { /*Nothing*/ }
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
        this.activityType = activityType
        val physicalActivities = activitiesOnDb.filter { activity ->
            activity.date in startDate..endDate && activity.getActivityTypeName() == activityType
        }

        return physicalActivities
    }

    fun handleSelectedMonth(activityType: ActivityType): List<PhysicalActivity> {
        val month = if (numberMonth <= 9){
            "0$numberMonth"
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

        return if (type == "DAY") {
            endDate = startFormattedDate
            startFormattedDate
        } else  { // if (type == "WEEK")
            calendar.add(Calendar.DAY_OF_MONTH, 6) // Add 6 days
            endDate = sdf.format(calendar.time)
            "$startFormattedDate / $endDate"
        }

    }

    fun showActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart): BarChart {

        return if (actualType == "DAY"){
            showDailyActivities(activitiesToShow, barChart)
        } else{ // if (actualType == "WEEK")
            showWeeklyActivities(activitiesToShow, barChart)
        }
    }

    private fun showDailyActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart): BarChart {
        val entries = ArrayList<BarEntry>()
        val sums = Array(24) { 0.0 }

        // Calculate sums per hour
        for (activities in activitiesToShow) {
            val hourStart = activities.start.substring(11, 13).toInt()
            val hourEnd = activities.end.substring(11, 13).toInt()
            val durationInHour = activities.duration / 3600.0

            for (i in hourStart until hourEnd+1) {
                sums[i] += durationInHour
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChart.description.text = "Daily Hours"

        return configureBarChart(barChart, barData, emptyArray(), isWeeklyChart = false, isMonthChart = false, isYearChart = false)

    }

    private fun showWeeklyActivities(activitiesToShow: List<PhysicalActivity>, barChart: BarChart) : BarChart{
        val referenceDates = getWeeklyDates()

        val entries = ArrayList<BarEntry>()
        val sums = Array(7) { 0.0 }

        // Calculate sums per day
        for (activities in activitiesToShow) {
            val durationInHour = activities.duration / 3600.0
            for (i in referenceDates.indices) {
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

        return configureBarChart(barChart, barData, referenceDates, isWeeklyChart = true, isMonthChart = false, isYearChart = false)


    }

    private fun configureBarChart(barChart: BarChart, barData: BarData, referenceDates: Array<String>, isWeeklyChart: Boolean, isMonthChart: Boolean, isYearChart: Boolean) : BarChart {

        // Configure BarChart
        barChart.setFitBars(true)
        barChart.data = barData
        barChart.animateY(2000)

        // Configure X axis
        barChart.xAxis.apply {
            setDrawAxisLine(false) // Disable X axis line
            setDrawGridLines(false) // Disable X axis grid lines
            if (isMonthChart || isWeeklyChart) {
                lateinit var dates: Array<String>
                if (isWeeklyChart) { // Weekly view
                    dates = Array(7) { "" }
                    for (i in referenceDates.indices) {
                        dates[i] = referenceDates[i].substring(6)
                    }
                } else { // Monthly view
                    dates = Array(referenceDates.size) { "" }
                    for (i in referenceDates.indices) {
                        dates[i] = referenceDates[i]
                    }
                }

                valueFormatter = IndexAxisValueFormatter(dates)

            } else if (isYearChart){
                val months = Array(12) { (it+1).toString() }

                valueFormatter = IndexAxisValueFormatter(months)

            } else { //Daily
                val hours = Array(24) { (it).toString() }

                valueFormatter =
                    IndexAxisValueFormatter(hours)
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
        barDataSet.color = getColorByType()
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.barBorderWidth = 0f
        barDataSet.setDrawValues(false) // Disable values above bars

        // Create BarData and configure
        val barData = BarData(barDataSet)
        barData.barWidth = 1.1f // Width of individual bars
        barData.isHighlightEnabled = false // Disable bar highlights

        return barData
    }

    private fun getColorByType(): Int {
        return when (activityType) {
            ActivityType.WALKING -> {
                Color.GREEN
            }
            ActivityType.DRIVING -> {
                Color.BLUE
            }
            else -> {
                Color.RED
            }
        }
    }

    private fun getWeeklyDates(): Array<String> {
        calendar.time = startSelectedDate
        val referenceDates = Array(7) {
            val currentDate = calendar.time
            val formattedDate = sdf.format(currentDate)
            calendar.add(Calendar.DAY_OF_MONTH, 1) // Add one day for next iteration
            formattedDate
        }
        return referenceDates
    }

    fun setMonthDates(numberSendMonth: Int) {
        // Get current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)


        days = when (numberSendMonth) {
            4, 6, 9, 11 -> 30 // 30 days: April, June, September, November
            2 -> if (isLeapYear(currentYear)) 29 else 28 // 28 or 29 days: February
            else -> 31 // 31 days: others
        }
        numberMonthString = ""

        if (numberSendMonth <= 9) {
            numberMonthString = "0$numberSendMonth"
        }
        this.numberMonth = numberSendMonth

    }

    private fun isLeapYear(year: Int): Boolean {
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
        for (activity in activitiesToShow) {
            val durationInHour = activity.duration / 3600.0
            for (i in 0 until days) {
                val day = if (i in 0..9) {
                    activity.date.substring(9) // days from 1 to 9
                } else {
                    activity.date.substring(8) // days from 10
                }
                if (activity.date.substring(5, 7) == numberMonthString && day == (i + 1).toString()) {
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
        return configureBarChart(barChart, barData, referenceDates, isWeeklyChart = false, isMonthChart = true, isYearChart = false)
    }

    fun getYearsToShow(): Array<String> {
        return (currentYear downTo currentYear - 9).map { it.toString() }.toTypedArray()
    }


    fun handleSelectedYear(selectedYear: String, activityType: ActivityType): List<PhysicalActivity> {
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
                    currentMonth = "0$i"
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
        return configureBarChart(barChart, barData, emptyArray(), isWeeklyChart = false, isMonthChart = false, isYearChart = true)
    }

    fun showWalkingActivitiesSteps(walkingActivitiesToShow: List<WalkingActivity>, barChartSteps: BarChart): BarChart {
        return if (actualType == "DAY"){
            showDailyActivitiesSteps(walkingActivitiesToShow, barChartSteps)
        } else{ // if (actualType == "WEEK")
            showWeeklyActivitiesSteps(walkingActivitiesToShow, barChartSteps)
        }

    }

    private fun showDailyActivitiesSteps(
        activitiesToShow: List<WalkingActivity>,
        barChartSteps: BarChart
    ): BarChart {
        val entries = ArrayList<BarEntry>()
        val sums = LongArray(24) { 0L }

        // Calculate sums per hour
        for (activity in activitiesToShow) {
            val hourStart = activity.start.substring(11, 13).toInt()
            val hourEnd = activity.end.substring(11, 13).toInt()
            val steps = activity.getSteps()

            for (i in hourStart until hourEnd+1) {
                sums[i] += steps
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))

        }
        Log.d("ENTRIES", entries.toString())

        val barData = configureBarData(entries)

        barChartSteps.description.text = "Daily Steps"

        return configureBarChart(barChartSteps, barData, emptyArray(), isWeeklyChart = false, isMonthChart = false, isYearChart = false)

    }


    private fun showWeeklyActivitiesSteps(activitiesToShow: List<WalkingActivity>, barChartSteps: BarChart) : BarChart{
        val referenceDates = getWeeklyDates()

        val entries = ArrayList<BarEntry>()
        val sums = Array(7) { 0L }

        // Calculate sums per day
        for (activities in activitiesToShow) {
            val steps = activities.getSteps()
            for (i in referenceDates.indices) {
                if (activities.date == referenceDates[i]) {
                    sums[i] += steps
                }
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChartSteps.description.text = "Daily Steps in Week"

        return configureBarChart(barChartSteps, barData, referenceDates, isWeeklyChart = true, isMonthChart = false, isYearChart = false)


    }

    fun showMonthActivitiesStep(activitiesToShow: List<WalkingActivity>, barChartStep: BarChart): BarChart {
        val entries = ArrayList<BarEntry>()

        val sums = Array(days) { 0L }
        val referenceDates = Array(days) { i ->
            if (i <= 8) {
                "0" + (i + 1)
            } else (i + 1).toString()
        }

        // Calculate sums per day
        for (activity in activitiesToShow) {
            val steps = activity.getSteps()
            for (i in 0 until days) {
                val day = if (i in 0..9) {
                    activity.date.substring(9) // days from 1 to 9
                } else {
                    activity.date.substring(8) // days from 10
                }

                if (activity.date.substring(5, 7) == numberMonthString && day == i.toString()) {
                    sums[i] += steps
                }
            }
        }






        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChartStep.description.text = "Daily Hours in Month"
        return configureBarChart(barChartStep, barData, referenceDates, isWeeklyChart = false, isMonthChart = true, isYearChart = false)
    }

    fun showYearActivitiesSteps(activitiesToShow: List<WalkingActivity>, barChartSteps: BarChart): BarChart {
        val entries = ArrayList<BarEntry>()

        val sums = Array(12) { 0L }

        // Calculate sums per day
        for (activity in activitiesToShow) {
            val steps = activity.getSteps()
            for (i in 1 until 13) {
                var currentMonth = i.toString()
                if (i <= 9){
                    currentMonth = "0$i"
                }
                if (activity.date.substring(0, 4) == currentYear.toString() && activity.date.substring(5,7) == currentMonth) {
                    sums[i-1] += steps
                }
            }
        }

        // Create BarEntry objects
        for (i in sums.indices) {
            entries.add(BarEntry(i.toFloat(), sums[i].toFloat()))
        }

        val barData = configureBarData(entries)

        barChartSteps.description.text = "Month Hours in Year"
        return configureBarChart(barChartSteps, barData, emptyArray(), isWeeklyChart = false, isMonthChart = false, isYearChart = true)
    }

}