package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentSettingsBinding
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchDailyReminder: Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchInactivityReminder: Switch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeViews()
        return root
    }

    private fun initializeViews() {
        // Initialize switches
        switchDailyReminder = binding.root.findViewById(R.id.switch_notification_daily_reminder)
        switchInactivityReminder = binding.root.findViewById(R.id.switch_notification_inactivity_reminder)

        // Load saved preferences
        val sharedPreferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferences.getBoolean("daily_reminder_enabled", false)

        // Set switches state based on saved preferences
        switchDailyReminder.isChecked = dailyReminderEnabled

        // Handle switch state changes
        switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("daily_reminder_enabled", isChecked)
            editor.apply()

            if (isChecked) {
                // Show time picker dialog when daily reminder switch is turned on
                showTimePickerDialog()
            } else {
                // Cancel daily notification when daily reminder switch is turned off
                cancelDailyNotification()
            }
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Create and show time picker dialog
        val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            // Format selected time in AM/PM format
            val formattedTime = formatTime(selectedHour, selectedMinute)
            // Schedule daily notification with formatted time
            scheduleDailyNotification(selectedHour, selectedMinute, formattedTime)
        }, hour, minute, false) // Use false to display in 12-hour format

        timePickerDialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        // Format time in AM/PM format
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun scheduleDailyNotification(hour: Int, minute: Int, formattedTime: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Create an intent for DailyReminderReceiver
        val intent = Intent(requireContext(), DailyReminderReceiver::class.java)
        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to schedule the notification
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        // Save the formatted notification time in SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("daily_reminder_hour", hour)
        editor.putInt("daily_reminder_minute", minute)
        editor.putString("daily_reminder_formatted_time", formattedTime)
        editor.apply()
    }

    private fun cancelDailyNotification() {
        // Create an intent for DailyReminderReceiver
        val intent = Intent(requireContext(), DailyReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
