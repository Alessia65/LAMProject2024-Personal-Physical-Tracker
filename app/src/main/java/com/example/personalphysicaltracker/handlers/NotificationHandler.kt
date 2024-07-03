package com.example.personalphysicaltracker.handlers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.personalphysicaltracker.receivers.DailyReminderReceiver
import com.example.personalphysicaltracker.receivers.StepsReminderReceiver
import com.example.personalphysicaltracker.utils.Constants
import java.util.Calendar

object NotificationHandler {

    fun createDailyReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Verifica se il canale esiste già
            val existingChannel = notificationManager.getNotificationChannel(Constants.CHANNEL_DAILY_REMINDER_ID)
            if (existingChannel == null) {
                //Daily Reminder
                val nameDailyReminder = Constants.CHANNEL_DAILY_REMINDER_TITLE
                val descriptionTextDailyReminder = Constants.CHANNEL_DAILY_REMINDER_DESCRIPTION
                val importanceDailyReminder = NotificationManager.IMPORTANCE_HIGH
                val channelDailyReminder = NotificationChannel(
                    Constants.CHANNEL_DAILY_REMINDER_ID,
                    nameDailyReminder,
                    importanceDailyReminder
                ).apply {
                    description = descriptionTextDailyReminder
                }

                notificationManager.createNotificationChannel(channelDailyReminder)
                Log.d("NOTIFICATION HANDLER", "daily reminder notification channel created")
            }
        }
    }


    fun createStepsReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Verifica se il canale esiste già
            val existingChannel = notificationManager.getNotificationChannel(Constants.CHANNEL_STEPS_REMINDER_ID)
            if (existingChannel == null) {
                val nameStepsReminder = Constants.CHANNEL_STEPS_REMINDER_TITLE
                val descriptionTextStepsReminder = Constants.CHANNEL_STEPS_REMINDER_DESCRIPTION
                val importanceStepsReminder = NotificationManager.IMPORTANCE_HIGH
                val channelStepsReminder = NotificationChannel(
                    Constants.CHANNEL_STEPS_REMINDER_ID,
                    nameStepsReminder,
                    importanceStepsReminder
                ).apply {
                    description = descriptionTextStepsReminder
                }

                notificationManager.createNotificationChannel(channelStepsReminder)
                Log.d("NOTIFICATION HANDLER", "steps reminder notification channel created")
            }
        }
    }



    fun scheduleDailyNotificationIfEnabled(context: Context) {
        createDailyReminderChannel(context)

        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)

        if (dailyReminderEnabled) {

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, DailyReminderReceiver::class.java)

                val hour = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_HOUR, 8) // Default hour: 8
                val minute = sharedPreferences.getInt(Constants.SHARED_PREFERENCES_DAILY_REMINDER_MINUTE, 0) // Default minute: 0

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    Constants.REQUEST_CODE_DAILY_REMINDER,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )


                Log.d("NOTIFICATION HANDLER", "New Alarm Created (daily reminder) at $hour:$minute")

        }
    }





    fun scheduleStepsNotificationIfEnabled(context: Context) {
        createStepsReminderChannel(context)
        val sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)

        if (stepsReminderEnabled) {

            // Calcola la data di trigger per la notifica
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, Constants.SHARED_PREFERENCES_STEPS_REMINDER_HOUR)
                set(Calendar.MINUTE, Constants.SHARED_PREFERENCES_STEPS_REMINDER_MINUTE)
                set(Calendar.SECOND, 0)
            }

            val intent = Intent(context, StepsReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                Constants.REQUEST_CODE_STEPS_REMINDER,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE // Check if PendingIntent already exists
            )


            Log.d("NOTIFICATION", "pending intent null")
            // PendingIntent non esiste, quindi crea un nuovo allarme

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d("NOTIFICATION HANDLER", "New Alarm Created (steps reminder)")


        }
    }


    fun cancelDailyNotification(context: Context) {

        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, DailyReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_DAILY_REMINDER, //0 per daily reminder
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (pendingIntent!=null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NOTIFICATION HANDLER", "daily notification canceled")

        } else {
            Log.d("NOTIFICATION HANDLER", "daily notification not canceled")
        }

        cancelDailyNotificationChannel(context)
    }

    fun cancelStepsNotification(context: Context){
        // Create an intent for DailyReminderReceiver
        val intent = Intent(context, StepsReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.REQUEST_CODE_STEPS_REMINDER, //1 per steps
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (pendingIntent!=null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NOTIFICATION HANDLER", "steps notification canceled")

        } else {
            Log.d("NOTIFICATION HANDLER", "steps notification not canceled")
        }

        cancelStepsNotificationChannel(context)

    }


    fun cancelDailyNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(Constants.CHANNEL_DAILY_REMINDER_ID)
            Log.d("NOTIFICATION HANDLER", "Daily notification channel deleted")
        }

    }

    fun cancelStepsNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(Constants.CHANNEL_STEPS_REMINDER_ID)
            Log.d("NOTIFICATION HANDLER", "Steps notification channel deleted")
        }

    }

}