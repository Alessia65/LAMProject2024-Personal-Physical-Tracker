package com.example.personalphysicaltracker

object Constants {
    const val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    const val REQUEST_CODE_DAILY_REMINDER = 0
    const val REQUEST_CODE_STEPS_REMINDER = 1

    const val CHANNEL_DAILY_REMINDER_TITLE = "Daily Reminder Channel"
    const val CHANNEL_DAILY_REMINDER_DESCRIPTION = "Channel for daily reminder notifications"
    const val CHANNEL_DAILY_REMINDER_ID = "daily_reminder_channel"

    const val CHANNEL_STEPS_REMINDER_TITLE = "Steps Reminder Channel"
    const val CHANNEL_STEPS_REMINDER_DESCRIPTION = "Channel for steps reminder notifications"
    const val CHANNEL_STEPS_REMINDER_ID = "steps_reminder_channel"


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




}