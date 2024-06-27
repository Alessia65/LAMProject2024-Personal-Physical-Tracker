package com.example.personalphysicaltracker.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.personalphysicaltracker.Constants
import com.example.personalphysicaltracker.PermissionsHandler
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentSettingsBinding
import com.example.personalphysicaltracker.receivers.ActivityTransitionReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel = SettingsViewModel()

    private lateinit var switchDailyReminder: Switch
    private lateinit var switchStepsReminder: Switch
    private lateinit var switchActivityRecognition: Switch

    private var dailySteps = 0L



    private lateinit var textCurrentActivity: TextView //Todo: cancellare

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        textCurrentActivity = binding.root.findViewById(R.id.eliminare) //Todo: cancellare

        initializeViews()

        return root
    }



    private fun initializeViews() {
        // Initialize switches
        switchDailyReminder = binding.root.findViewById(R.id.switch_notification_daily_reminder)
        switchStepsReminder = binding.root.findViewById(R.id.switch_notification_steps_reminder)
        switchActivityRecognition = binding.root.findViewById(R.id.switch_b_o_activity_recognition)

        val sharedPreferencesDaily = requireContext().getSharedPreferences(Constants.SHARED_PREFERENCES_DAILY_REMINDER, Context.MODE_PRIVATE)
        val dailyReminderEnabled = sharedPreferencesDaily.getBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)
        switchDailyReminder.isChecked = dailyReminderEnabled

        val sharedPreferencesSteps = requireContext().getSharedPreferences(Constants.SHARED_PREFERENCES_STEPS_REMINDER, Context.MODE_PRIVATE)
        val stepsReminderEnabled = sharedPreferencesSteps.getBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)
        switchStepsReminder.isChecked = stepsReminderEnabled

        // Handle switch state changes
        switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            handleDailyReminderSwitch(isChecked, sharedPreferencesDaily)
        }

        switchStepsReminder.setOnCheckedChangeListener { _, isChecked ->
            handleStepReminderSwitch(isChecked ,sharedPreferencesSteps)
        }


        //Background Operations

        switchActivityRecognition.isChecked = settingsViewModel.checkBackgroundRecogniseActivitiesOn(requireContext())

        switchActivityRecognition.setOnCheckedChangeListener{_, isChecked ->
            handleActivityRecognitionSwitch(isChecked)
        }

        calculateDailySteps()

    }

    private fun handleActivityRecognitionSwitch(isChecked: Boolean) {
        settingsViewModel.setBackgroundRecogniseActivies(requireContext(), isChecked)
        if (isChecked) {
            ActivityTransitionHandler.connect()
            registerActivityTransitions()
        }else {
            ActivityTransitionHandler.disconnect(requireContext())
            unregisterActivityTransitions()
        }
    }

    private fun handleStepReminderSwitch(isChecked: Boolean, sharedPreferencesSteps: SharedPreferences) {
        val editor = sharedPreferencesSteps.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, isChecked)
        editor.apply()

        if (isChecked) {
            if (checkNotificationPermission()){
                showNumberOfStepsDialog()
            }
            else{
                PermissionsHandler.requestPermissions(requireActivity())
                if (!PermissionsHandler.notificationPermission ){
                    switchStepsReminder.isChecked = false
                    val editor = sharedPreferencesSteps.edit()
                    editor.putBoolean(Constants.SHARED_PREFERENCES_STEPS_REMINDER_ENABLED, false)
                    editor.apply()
                }

            }

        } else {
            // Cancel inactivity notification when inactivity reminder switch is turned off
            settingsViewModel.cancelStepsNotification(requireContext())
        }
    }

    private fun handleDailyReminderSwitch(isChecked: Boolean, sharedPreferencesDaily: SharedPreferences) {
        val editor = sharedPreferencesDaily.edit()
        editor.putBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, isChecked)
        editor.apply()

        if (isChecked) {
            // Show time picker dialog when daily reminder switch is turned on
            if (checkNotificationPermission()){
                showTimePickerDialog()
            }
            else{
                PermissionsHandler.requestPermissions(requireActivity())
                if (!PermissionsHandler.notificationPermission){
                    switchDailyReminder.isChecked = false
                    val editor = sharedPreferencesDaily.edit()
                    editor.putBoolean(Constants.SHARED_PREFERENCES_DAILY_REMINDER_ENABLED, false)
                    editor.apply()
                }
            }
        } else {
            // Cancel daily notification when daily reminder switch is turned off
            settingsViewModel.cancelDailyNotification(requireContext())
        }
    }

    private fun registerActivityTransitions() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                ActivityTransitionHandler.registerActivityTransitions()
            }
        }
    }

    private fun unregisterActivityTransitions() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                ActivityTransitionHandler.unregisterActivityTransitions()
            }
        }
    }





    private fun checkNotificationPermission(): Boolean {
        return PermissionsHandler.notificationPermission
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
            .setTitle("Pick steps number")
            .setPositiveButton("OK") { dialog, which ->
                // Ottieni il valore selezionato
                val selectedNumber = displayValues[numberPicker.value - 1].toInt()
                // Fai qualcosa con il numero selezionato
                Log.d("SelectedNumber", "Selected number is $selectedNumber")

                scheduleStepsNotification(selectedNumber)


            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
                switchStepsReminder.isChecked = false
            }.setCancelable(false)

        // Mostra il dialog
        val alertDialog = builder.create()



        alertDialog.show()
    }

    private fun scheduleStepsNotification(selectedNumber: Int) {
        // Calcola la data di trigger per la notifica
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Constants.SHARED_PREFERENCES_STEPS_REMINDER_HOUR)
            set(Calendar.MINUTE, Constants.SHARED_PREFERENCES_STEPS_REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
        }

        calculateDailySteps()
        settingsViewModel.scheduleStepsNotification(selectedNumber,calendar, dailySteps, requireContext())

        val date: Date = calendar.time
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val formattedDate: String = sdf.format(date)
        Toast.makeText(requireContext(), "Steps reminder set for $formattedDate if goal not met.", Toast.LENGTH_SHORT).show()

    }

    private fun calculateDailySteps() {

        settingsViewModel.getTotalStepsFromToday(this.activity, this) { totalSteps ->
            this.dailySteps = totalSteps
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

        timePickerDialog.setOnCancelListener {
            switchDailyReminder.isChecked = false
        }

        timePickerDialog.setCanceledOnTouchOutside(false)
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

        settingsViewModel.scheduleDailyNotification(calendar, requireContext(), hour, minute, formattedTime)
        Toast.makeText(requireContext(), "Daily reminder set for $formattedTime", Toast.LENGTH_SHORT).show()

    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

