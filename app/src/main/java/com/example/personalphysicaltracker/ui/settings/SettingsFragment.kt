package com.example.personalphysicaltracker.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentSettingsBinding
import com.example.personalphysicaltracker.handlers.ActivityTransitionHandler
import com.example.personalphysicaltracker.handlers.GeofenceHandler
import com.example.personalphysicaltracker.handlers.LocationHandler
import com.example.personalphysicaltracker.viewModels.SettingsViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import org.osmdroid.config.Configuration.*
import java.util.Date
import java.util.Locale

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel = SettingsViewModel()

    private lateinit var switchDailyReminder: Switch
    private lateinit var switchStepsReminder: Switch
    private lateinit var switchActivityRecognition: Switch
    private lateinit var switchLocation: Switch
    private lateinit var setLocation: TextView

    private var dailySteps = 0L
    private val SETTINGS_PERMISSION_REQUEST = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        // Check if activity recognition permission is granted
        if (PermissionsHandler.hasLocationPermissions(requireContext())){
            showDialogSettings()
            PermissionsHandler.locationPermission = false
        } else {
            PermissionsHandler.locationPermission = true
        }
    }

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
        switchActivityRecognition = binding.root.findViewById(R.id.switch_b_o_activity_recognition)
        switchLocation = binding.root.findViewById(R.id.switch_b_o_location)
        setLocation = binding.root.findViewById(R.id.set_location_text)

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

        switchLocation.isChecked = settingsViewModel.checkBackgroundLocationDetection(requireContext())
        switchLocation.setOnCheckedChangeListener{_, isChecked ->
            handleLocationDetectionSwitch(isChecked)
        }


        setLocation.setOnClickListener{
            handlePermissions()
            if (PermissionsHandler.hasLocationPermissions(requireContext())){
                goToFragmentMap()
            }
        }

        calculateDailySteps()

    }

    private fun handleLocationDetectionSwitch(isChecked: Boolean) {
        settingsViewModel.setBackgroundLocationDetection(requireContext(), isChecked)
        if (isChecked) {
            handlePermissions()
            val settedLocation = checkLocation()
            if (settedLocation){
                LocationHandler.startLocationUpdates(requireContext(), LocationServices.getFusedLocationProviderClient(requireActivity()), settingsViewModel.getActivityViewModel())
            } else {
               switchLocation.isChecked = false
                settingsViewModel.setBackgroundLocationDetection(requireContext(), false)
                showGeofenceAlert()
                LocationHandler.stopLocationUpdates(LocationServices.getFusedLocationProviderClient(requireActivity()))
            }

        } else {
            LocationHandler.stopLocationUpdates(LocationServices.getFusedLocationProviderClient(requireActivity()))
            // Handle case when switch is unchecked
            lifecycleScope.launch(Dispatchers.IO) {
                GeofenceHandler.deregisterGeofence()
            }
            GeofenceHandler.removeGeofence("selected_location")
        }
    }

    private fun checkLocation(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences(Constants.GEOFENCE, Context.MODE_PRIVATE)
        val key = sharedPreferences.getString(Constants.GEOFENCE_KEY, null)
        return !key.isNullOrEmpty()
    }



    private fun handleActivityRecognitionSwitch(isChecked: Boolean) {
        settingsViewModel.setBackgroundRecogniseActivies(requireContext(), isChecked)

        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                Log.d("SETTINGS", "here S")
                if (PermissionsHandler.hasLocationPermissions(requireContext())) {
                    ActivityTransitionHandler.connect()
                    registerActivityTransitions()
                } else {
                    PermissionsHandler.requestLocationPermissions(requireActivity(), requireContext())
                    if (!PermissionsHandler.locationPermission){
                        settingsViewModel.setBackgroundRecogniseActivies(requireContext(), false)
                        switchActivityRecognition.isChecked = false
                    }
                }
            } else {
                Log.d("SETTINGS", "here")
                ActivityTransitionHandler.connect()
                registerActivityTransitions()
            }

        }else {
            ActivityTransitionHandler.disconnect()
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
        Log.d("SETTINGS", "notification permission is: " + PermissionsHandler.notificationPermission)
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
            .setPositiveButton("OK") { _, _ ->
                // Ottieni il valore selezionato
                val selectedNumber = displayValues[numberPicker.value - 1].toInt()
                // Fai qualcosa con il numero selezionato
                Log.d("SelectedNumber", "Selected number is $selectedNumber")

                scheduleStepsNotification(selectedNumber)


            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                switchStepsReminder.isChecked = false
            }.setCancelable(false)

        // Mostra il dialog
        val alertDialog = builder.create()



        alertDialog.show()
    }

    private fun scheduleStepsNotification(selectedNumber: Int) {

        calculateDailySteps()
        settingsViewModel.scheduleStepsNotification(selectedNumber, requireContext())



        // Calcola la data di trigger per la notifica
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Constants.SHARED_PREFERENCES_STEPS_REMINDER_HOUR)
            set(Calendar.MINUTE, Constants.SHARED_PREFERENCES_STEPS_REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
        }


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
        settingsViewModel.scheduleDailyNotification(requireContext(), hour, minute, formattedTime)
        Toast.makeText(requireContext(), "Daily reminder set for $formattedTime", Toast.LENGTH_SHORT).show()
    }




    private fun handlePermissions() {
        if (PermissionsHandler.hasLocationPermissions(requireContext())){
            return
        }

        if (!PermissionsHandler.requestLocationPermissions(requireActivity(), requireContext())){
            showDialogSettings()
            switchLocation.isChecked = false
            settingsViewModel.setBackgroundLocationDetection(requireContext(), false)

        }

    }

    private fun showDialogSettings(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("You need permissions to detect locations!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.setData(uri)
                SETTINGS_PERMISSION_REQUEST.launch(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
                PermissionsHandler.locationPermission = false
            }
        builder.show()
    }

    private fun goToFragmentMap(){
        val navController = findNavController()
        navController.navigate(R.id.mapFragment)

    }

    private fun showGeofenceAlert() {
        val title = "Ops! Something's missing"
        val message = "You need to set your interest's location"


        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

