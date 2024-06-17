package com.example.personalphysicaltracker.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.activities.WalkingActivity

//Classe per accedere al DB
class ActivityViewModel(private val repository: TrackingRepository) : ViewModel() {


    fun getAllActivities(): LiveData<List<ActivityEntity>> {
        return repository.getAllActivities()
    }

    fun insertActivityEntity (activityEntity: ActivityEntity) {
        repository.insertActivityEntity(activityEntity)
    }


    suspend fun getLastActivity(): ActivityEntity? {
        return repository.getLastActivity()
    }

    suspend fun getTotalDurationByActivityTypeInDay(day: String, activityType: String): Double {
        return repository.getTotalDurationByActivityTypeInDay(day, activityType)
    }

    suspend fun getLastWalkingId(): Int{
        return repository.getLastWalkingId()
    }

    suspend fun insertWalkingActivityEntity(id: Int, steps: Long){
        return repository.insertWalkingActivityEntity(id, steps)
    }

    suspend fun getTotalStepsFromToday(date:String): Long{
        return repository.getTotalStepsFromToday(date)
    }
}

//TODO: Che fa???
class ActivityViewModelFactory(private val repository: TrackingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}