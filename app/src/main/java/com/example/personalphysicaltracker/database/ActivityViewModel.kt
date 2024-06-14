package com.example.personalphysicaltracker.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mvvm_todolist.TrackingRepository

class ActivityViewModel(private val repository: TrackingRepository) : ViewModel() {


    fun getAllActivities(): LiveData<List<ActivityEntity>> {
        return repository.getAllActivities()
    }

    fun insertActivityEntity (activityEntity: ActivityEntity) {
        repository.insertActivityEntity(activityEntity)
    }

    fun deleteListActivities (toDelete: List<ActivityEntity>) {
        repository.deleteListActivities(toDelete)
    }
}

//Che fa???
class ActivityViewModelFactory(private val repository: TrackingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}