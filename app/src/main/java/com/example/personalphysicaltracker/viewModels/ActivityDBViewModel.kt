package com.example.personalphysicaltracker.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.LocationInfo
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.database.ActivityEntity
import com.example.personalphysicaltracker.database.LocationEntity
import com.example.personalphysicaltracker.database.TrackingRepository
import com.example.personalphysicaltracker.database.WalkingActivityEntity

class ActivityDBViewModel(private val repository: TrackingRepository) : ViewModel() {


    fun insertActivityEntity (activityEntity: ActivityEntity) {
        repository.insertActivityEntity(activityEntity)
    }


    suspend fun getLastActivity(): ActivityEntity? {
        return repository.getLastActivity()
    }

    suspend fun getTotalDurationByActivityTypeInDay(day: String, activityType: String): Double {
        return repository.getTotalDurationByActivityTypeInDay(day, activityType)
    }

    suspend fun insertWalkingActivity(activityEntity: ActivityEntity, steps: Long) {
        repository.insertWalkingActivity(activityEntity, steps)
    }

    suspend fun getTotalStepsFromToday(date:String): Long{
        return repository.getTotalStepsFromToday(date)
    }

    private suspend fun getListOfActivities(): List<ActivityEntity> {
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

    private suspend fun createWalkingActivity(activityEntity: ActivityEntity): WalkingActivity {
        val activity = WalkingActivity()
        activity.date = activityEntity.date
        activity.start = activityEntity.timeStart
        activity.end = activityEntity.timeFinish
        activity.duration = activityEntity.duration

        //Calculate steps:
        val id = activityEntity.id
        val steps = getWalkingActivityById(id).steps
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
    private suspend fun getWalkingActivityById(id: Int): WalkingActivityEntity {
        return repository.getWalkingActivityById(id)
    }

     fun insertLocationInfo(locationEntity: LocationEntity) {
        return repository.insertLocationInfo(locationEntity)
    }

    suspend fun getTotalPresenceInLocation(latitude: Double, longitude: Double, startDate:String, endDate:String): Double {
        return repository.getTotalPresenceInLocation(latitude, longitude, startDate, endDate)
    }

    suspend fun getAllLocationsInDate(startDate: String, endDate: String): List<LocationInfo> {
        val locationEntities = repository.getAllLocationsInDate(startDate, endDate)

        return obtainListLocation(locationEntities)

    }

    private fun obtainListLocation(locationEntities: List<LocationEntity>): List<LocationInfo> {
        val temp: MutableList<LocationInfo> = mutableListOf()

        for (entity in locationEntities) {
            val newLocation = LocationInfo()
            newLocation.date = entity.date
            newLocation.start = entity.timeStart
            newLocation.end = entity.timeFinish
            newLocation.latitude = entity.latitude
            newLocation.longitude = entity.longitude
            temp.add(newLocation)
        }
        return temp
    }



}



// Factory class for creating instances of ActivityViewModel
@Suppress("UNCHECKED_CAST")
class ActivityViewModelFactory(private val repository: TrackingRepository) : ViewModelProvider.Factory {

    // Method to create an instance of ViewModel
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Check if the ViewModel class is assignable from ActivityViewModel
        if (modelClass.isAssignableFrom(ActivityDBViewModel::class.java)) {


            // Return a new instance of ActivityViewModel with the provided repository
            return ActivityDBViewModel(repository) as T
        }

        // If the class is not recognized, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}