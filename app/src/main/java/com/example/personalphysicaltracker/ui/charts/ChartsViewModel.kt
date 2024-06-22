package com.example.personalphysicaltracker.ui.charts

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityViewModel
import com.example.personalphysicaltracker.database.ActivityViewModelFactory
import com.example.personalphysicaltracker.database.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ChartsViewModel : ViewModel() {

    private lateinit var activityViewModel: ActivityViewModel

    var activitiesOnDb: List<PhysicalActivity> = emptyList() //Da non modificare
    var sumsPieChart: Array<Float> = emptyArray()


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
        for (s in sumsPieChart){
            Log.d("SUM",s.toString())
        }
    }

    fun obtainDatesForDialog(): Array<Float>{
        return sumsPieChart
    }

    fun handleSelectedDateRangeWalking(startDate: String, endDate: String): List<WalkingActivity> {
        // Filtra le attività in base all'intervallo di date selezionato
        val walkingActivities = activitiesOnDb.filter { activity ->
            activity.date >= startDate && activity.date <= endDate &&
                    activity.getActivityTypeName() == ActivityType.WALKING
        }

        if (walkingActivities.isNotEmpty()){
            for(w in walkingActivities){
                Log.d("STampa W", w.date + ", " + w.duration)
            }
        } else if (walkingActivities.isEmpty()){
            Log.d("STampa W", "EMPTY")
        } else if (walkingActivities == null){
            Log.d("STampa W", "NULL")
        } else {
            Log.d("STampa W", "BOH")

        }

        return walkingActivities as List<WalkingActivity>
    }

    fun handleSelectedMonthWalking(numberMonth: Int): List<WalkingActivity> {
        var month = if (numberMonth <= 9){
            "0" + numberMonth
        } else {
            numberMonth.toString()
        }

        val walkingActivities = activitiesOnDb.filter { activity ->
            activity.date.substring(5,7) == month &&
                    activity.getActivityTypeName() == ActivityType.WALKING
        }

        if (walkingActivities.isNotEmpty()){
            for(w in walkingActivities){
                Log.d("STampa M", w.date + ", " + w.duration)
            }
        } else if (walkingActivities.isEmpty()){
            Log.d("STampa M", "EMPTY")
        } else if (walkingActivities == null){
            Log.d("STampa M ", "NULL")
        } else {
            Log.d("STampa M", "BOH")

        }


        return walkingActivities as List<WalkingActivity>



    }


    //TODO: caso in cui un'attività è a ridosso, la data viene inserita come data in cui finisce l'attività dovrei farla dividere giornalemnte
}