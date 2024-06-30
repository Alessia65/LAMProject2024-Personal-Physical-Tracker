package com.example.personalphysicaltracker.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.database.WalkingActivityEntity

class ActivityViewModel(private val repository: TrackingRepository) : ViewModel() {


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

    suspend fun insertWalkingActivity(activityEntity: ActivityEntity, steps: Long) {
        repository.insertWalkingActivity(activityEntity, steps)
    }

    suspend fun getTotalStepsFromToday(date:String): Long{
        return repository.getTotalStepsFromToday(date)
    }

    suspend fun getListOfActivities(): List<ActivityEntity> {
        return repository.getListOfActivities()
    }

    suspend fun getListOfPhysicalActivities(): List<PhysicalActivity> {
        val activityEntities: List<ActivityEntity> = getListOfActivities()
        val physicalActivities = mutableListOf<PhysicalActivity>()
        for (activityEntity in activityEntities) {
            when (activityEntity.activityType) {
                "WALKING" -> {
                    physicalActivities.add(createWalkingActivity(activityEntity))
                }
                "DRIVING" -> {
                    physicalActivities.add(createDrivingActivity(activityEntity))
                }
                "STANDING" -> {
                    physicalActivities.add(createStandingActivity(activityEntity))
                }
                else -> {
                    physicalActivities.add(createPhysicalActivity(activityEntity))
                }
            }
        }
        return physicalActivities
    }

    suspend private fun createWalkingActivity(activityEntity: ActivityEntity): WalkingActivity {
        val activity = WalkingActivity()
        activity.date = activityEntity.date
        activity.start = activityEntity.timeStart
        activity.end = activityEntity.timeFinish
        activity.duration = activityEntity.duration

        //Calculate steps:
        val id = activityEntity.id
        var steps = 0L
        if (getWalkingActivityById(id)!=null) {
            steps = getWalkingActivityById(id).steps
        }
        activity.setActivitySteps(steps)
        return activity
    }

    private fun createDrivingActivity(activityEntity: ActivityEntity): DrivingActivity {
        val activity = DrivingActivity()
        activity.date = activityEntity.date
        activity.start = activityEntity.timeStart
        activity.end = activityEntity.timeFinish
        activity.duration = activityEntity.duration

        return activity
    }

    private fun createStandingActivity(activityEntity: ActivityEntity): StandingActivity {
        val activity = StandingActivity()
        activity.date = activityEntity.date
        activity.start = activityEntity.timeStart
        activity.end = activityEntity.timeFinish
        activity.duration = activityEntity.duration

        return activity
    }

    private fun createPhysicalActivity(activityEntity: ActivityEntity): PhysicalActivity {
        val activity = PhysicalActivity()
        activity.date = activityEntity.date
        activity.start = activityEntity.timeStart
        activity.end = activityEntity.timeFinish
        activity.duration = activityEntity.duration

        return activity
    }
    suspend private fun getWalkingActivityById(id: Int): WalkingActivityEntity {
        return repository.getWalkingActivityById(id)
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