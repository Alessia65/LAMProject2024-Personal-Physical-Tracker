package com.example.personalphysicaltracker

object Constants {


    const val PERMISSION_REQUESTS_CODE = 100

    const val REQUEST_CODE_DAILY_REMINDER = 0
    const val REQUEST_CODE_STEPS_REMINDER = 1
    const val REQUEST_CODE_ACTIVITY_RECOGNITION = 2


    const val CHANNEL_DAILY_REMINDER_TITLE = "Daily Reminder Channel"
    const val CHANNEL_DAILY_REMINDER_DESCRIPTION = "Channel for daily reminder notifications"
    const val CHANNEL_DAILY_REMINDER_ID = "daily_reminder_channel"

    const val CHANNEL_STEPS_REMINDER_TITLE = "Steps Reminder Channel"
    const val CHANNEL_STEPS_REMINDER_DESCRIPTION = "Channel for steps reminder notifications"
    const val CHANNEL_STEPS_REMINDER_ID = "steps_reminder_channel"

    const val CHANNEL_ACTIVITY_RECOGNITION_TITLE = "Activity Recognition Channel"
    const val CHANNEL_ACTIVITY_RECOGNITION_DESCRIPTION = "Channel for activity recognition changes notifications"
    const val CHANNEL_ACTIVITY_RECOGNITION_ID = "activity_recognition_channel"


    const val SHARED_PREFERENCES_DAILY_REMINDER = "settings daily reminder notification"
    const val SHARED_PREFERENCES_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
    const val SHARED_PREFERENCES_DAILY_REMINDER_HOUR = "daily_reminder_hour"
    const val SHARED_PREFERENCES_DAILY_REMINDER_MINUTE = "daily_reminder_minute"
    const val SHARED_PREFERENCES_DAILY_REMINDER_FORMATTED_TIME = "daily_reminder_formatted_time"

    const val SHARED_PREFERENCES_STEPS_REMINDER = "settings minimum steps reminder notification"
    const val SHARED_PREFERENCES_STEPS_REMINDER_ENABLED = "steps_reminder_enabled"
    const val SHARED_PREFERENCES_STEPS_REMINDER_NUMBER = "steps_number"
    const val SHARED_PREFERENCES_STEPS_REMINDER_HOUR = 17
    const val SHARED_PREFERENCES_STEPS_REMINDER_MINUTE = 0
    const val SHARED_PREFERENCES_STEPS_DAILY = "steps_number_daily"

    const val SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION = "background activities recognition"
    const val SHARED_PREFERENCES_BACKGROUND_ACTIVITIES_RECOGNITION_ENABLED = "background_activities_recognition_enabled"

    const val SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION = "background location detection"
    const val SHARED_PREFERENCES_BACKGROUND_LOCATION_DETECTION_ENABLED = "background_location_detection_enabled"


    const val PERMISSION_ACTIVITY_RECOGNITION = android.Manifest.permission.ACTIVITY_RECOGNITION
    const val PERMISSION_ACTIVITY_RECOGNITION_BEFORE = "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
    const val PERMISSION_POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS

    const val BACKGROUND_OPERATION_ACTIVITY_RECOGNITION = "ACTIVITY_RECOGNITION_ON"
    const val BACKGROUND_OPERATION_ACTIVITY_RECOGNITION_CODE = 1000

    const val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = 30 * 60 * 1000
    const val GEOFENCE_RADIUS_IN_METERS = 100.0F

    const val BACKGROUND_OPERATION_LOCATION_DETECTION = "LOCATION_DETECTION_ON"
    const val BACKGROUND_OPERATION_LOCATION_DETECTION_CODE = 1001
    const val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
    const val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
    const val ACCESS_BACKGROUND_LOCATION = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    const val PERMISSION_LOCATION_REQUESTS_CODE = 101


    /*
    TODO:
    1. ogni volta che apro l'app e le notifiche sono attivate arriva la notifica
    2. registrazione dell'activity broadcast recever nel main
     */
}