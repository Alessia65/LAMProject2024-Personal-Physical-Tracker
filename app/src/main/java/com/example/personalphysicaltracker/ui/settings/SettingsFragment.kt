package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentSettingsBinding
import com.example.personalphysicaltracker.notifications.DailyReminderReceiver
import com.example.personalphysicaltracker.notifications.StepsReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel = SettingsViewModel()

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchDailyReminder: Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchStepsReminder: Switch
    private var dailySteps = 0L

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
        switchStepsReminder = binding.root.findViewById(R.id.switch_notification_steps_reminder)

        // Load saved preferences
        val sharedPreferencesDaily = requireContext().getSharedPreferences("settings daily reminder notification", Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferencesDaily.getBoolean("daily_reminder_enabled", false)

        val sharedPreferencesSteps = requireContext().getSharedPreferences("settings minimum steps reminder notification", Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferencesSteps.getBoolean("steps_reminder_enabled", false)

        // Set switches state based on saved preferences
        switchDailyReminder.isChecked = dailyReminderEnabled
        switchStepsReminder.isChecked = stepsReminderEnabled

        // Handle switch state changes
        switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferencesDaily.edit()
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

        switchStepsReminder.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferencesSteps.edit()
            editor.putBoolean("steps_reminder_enabled", isChecked)
            editor.apply()

            if (isChecked) {
                // Prompt user to set days of inactivity
                showNumberOfStepsDialog()
            } else {
                // Cancel inactivity notification when inactivity reminder switch is turned off
                cancelStepsNotification()
            }
        }
        calculateDailySteps()
    }

    private fun cancelStepsNotification() {
        // Create an intent for DailyReminderReceiver
        val intent = Intent(requireContext(), StepsReminderReceiver::class.java)
        // Create a PendingIntent to be cancelled
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1, //1 per steps
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun showNumberOfStepsDialog() {
        // Inflating the dialog layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)

        // Finding the NumberPicker
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)

        // Configurazione del NumberPicker (es. range, valore iniziale, ecc.)
        numberPicker.minValue = 1
        numberPicker.maxValue = (20000 - 200) / 200 // Calcola il numero di elementi
        val displayValues = (200..20000 step 200).toList().map { it.toString() }.toTypedArray() // Array di valori
        numberPicker.displayedValues = displayValues

        // Creazione del AlertDialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
            .setTitle("Seleziona un numero")
            .setPositiveButton("OK") { dialog, which ->
                // Ottieni il valore selezionato
                val selectedNumber = displayValues[numberPicker.value - 1].toInt()
                // Fai qualcosa con il numero selezionato
                Log.d("SelectedNumber", "Selected number is $selectedNumber")
                scheduleStepsNotification(selectedNumber)
            }
            .setNegativeButton("Annulla") { dialog, which ->
                dialog.dismiss()
            }

        // Mostra il dialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun scheduleStepsNotification(selectedNumber: Int) {
        // Calcola la data di trigger per la notifica
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        calculateDailySteps()
        // Verifica se la somma dei passi giornalieri è inferiore all'obiettivo selezionato
        val dailyStepsSum = dailySteps // Implementa questa funzione per calcolare la somma dei passi giornalieri

        if (dailyStepsSum < selectedNumber) {
            Log.d("StepsReminder", "Daily steps goal: $selectedNumber, current: $dailyStepsSum")
            // Invia la notifica degli steps solo se la somma è inferiore all'obiettivo
            createStepsNotification(calendar)
        } else {
            // Non fare nulla, la somma dei passi è sufficiente
            Log.d("StepsReminder", "Daily steps goal already met.")
        }

        // Salva l'obiettivo selezionato nelle SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("settings minimum steps reminder notification", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("steps_reminder", selectedNumber)
        editor.apply()
    }

    private fun calculateDailySteps() {

        settingsViewModel.getTotalStepsFromToday(this.activity, this) { totalSteps ->
            this.dailySteps = totalSteps
            /*
            lifecycleScope.launch {
            }

             */
        }
    }

    private fun createStepsNotification(calendar: Calendar) {

        // Create an intent for StepsReminderReceiver
        val intent = Intent(requireContext(), StepsReminderReceiver::class.java)
        // Create a PendingIntent to be triggered at the specified time
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1, // Codice per steps
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
        // Inform the user that the steps notification will be set for a specific time
        Toast.makeText(requireContext(), "Steps reminder set for 5:00 PM if goal not met.", Toast.LENGTH_SHORT).show()
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
            0, //0 per daily reminder
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

        Toast.makeText(requireContext(), "Daily reminder set for $formattedTime", Toast.LENGTH_SHORT).show()


        // Save the formatted notification time in SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("settings daily reminder notification", Context.MODE_PRIVATE)
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
            0, //0 per daily reminder
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager service to cancel the pending intent
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

