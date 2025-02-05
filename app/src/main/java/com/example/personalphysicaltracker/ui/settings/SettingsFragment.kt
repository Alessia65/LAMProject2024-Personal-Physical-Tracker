package com.example.personalphysicaltracker.ui.settings

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.personalphysicaltracker.utils.Constants
import com.example.personalphysicaltracker.handlers.PermissionsHandler
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentSettingsBinding
import com.example.personalphysicaltracker.viewModels.SettingsViewModel
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
    private lateinit var helpButton: TextView

    private var dailySteps = 0L
    private val SETTINGS_PERMISSION_REQUEST_LOCATION = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (!PermissionsHandler.hasLocationPermissions(requireActivity())){
            showDialogSettingsLocation()
        }
    }

    private val SETTINGS_PERMISSION_REQUEST_NOTIFICATION = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (!PermissionsHandler.checkPermissionNotifications(requireActivity())){
            showDialogSettingsNotifications()
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
        helpButton = binding.root.findViewById(R.id.help_button)
        helpButton.setOnClickListener{
            goToFragmentHelp()
        }
        switchDailyReminder = binding.root.findViewById(R.id.switch_notification_daily_reminder)
        switchStepsReminder = binding.root.findViewById(R.id.switch_notification_steps_reminder)
        switchActivityRecognition = binding.root.findViewById(R.id.switch_b_o_activity_recognition)
        switchLocation = binding.root.findViewById(R.id.switch_b_o_location)
        setLocation = binding.root.findViewById(R.id.set_location_text)

        checkPermissionsIfSettingsOn()


        switchDailyReminder.isChecked = settingsViewModel.checkDailyReminder(requireActivity())
        switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            handleDailyReminderSwitch(isChecked)
        }

         switchStepsReminder.isChecked = settingsViewModel.checkStepsReminder(requireActivity())
        switchStepsReminder.setOnCheckedChangeListener { _, isChecked ->
            handleStepReminderSwitch(isChecked)
        }


        switchActivityRecognition.isChecked = settingsViewModel.checkBackgroundRecogniseActivitiesOn(requireContext())
        switchActivityRecognition.setOnCheckedChangeListener{_, isChecked ->
            handleActivityRecognitionSwitch(isChecked)
        }

        switchLocation.isChecked =  settingsViewModel.checkBackgroundLocationDetection(requireContext())
        switchLocation.setOnCheckedChangeListener{_, isChecked ->
            handleLocationDetectionSwitch(isChecked)
        }
        setLocation.setOnClickListener{
            if (PermissionsHandler.hasLocationPermissions(requireActivity())){
                goToFragmentMap()
            } else {
                showDialogSettingsLocation()
            }
        }

        calculateDailySteps()

    }

    private fun handleDailyReminderSwitch(isChecked: Boolean) {
        settingsViewModel.setDailyReminder(requireActivity(), isChecked)

        if (isChecked) {
            if (PermissionsHandler.checkPermissionNotifications(requireActivity())){
                showTimePickerDialog()
            }
            else{
                PermissionsHandler.requestPermissionNotifications(requireActivity())
                if (!PermissionsHandler.checkPermissionNotifications(requireActivity())){
                    switchDailyReminder.isChecked = false
                    settingsViewModel.setDailyReminder(requireActivity(), false)
                    showDialogSettingsNotifications()
                }
            }
        } else {
            settingsViewModel.cancelDailyNotification(requireContext())
        }
    }

    private fun handleStepReminderSwitch(isChecked: Boolean) {
        settingsViewModel.setStepsReminder(requireActivity(), isChecked)

        if (isChecked) {
            if (PermissionsHandler.checkPermissionNotifications(requireActivity())){
                showNumberOfStepsDialog()
            }
            else{
                PermissionsHandler.requestPermissionNotifications(requireActivity())
                if (!PermissionsHandler.checkPermissionNotifications(requireActivity()) ){
                    switchStepsReminder.isChecked = false
                    settingsViewModel.setStepsReminder(requireActivity(), false)
                    showDialogSettingsNotifications()
                }

            }

        } else {
            settingsViewModel.cancelStepsNotification(requireContext())
        }
    }

    private fun handleLocationDetectionSwitch(isChecked: Boolean) {
        settingsViewModel.setBackgroundLocationDetection(requireActivity(), isChecked)
        if (isChecked) {
            val setLocation = settingsViewModel.checkLocationSet(requireActivity())
            if (setLocation && PermissionsHandler.hasLocationPermissions(requireContext()) && PermissionsHandler.checkPermissionNotifications(requireActivity())){
                settingsViewModel.startLocationUpdates(requireActivity())
            } else if (!setLocation){
               switchLocation.isChecked = false
                settingsViewModel.setBackgroundLocationDetection(requireActivity(), false)
                showGeofenceAlert()
                settingsViewModel.stopLocationUpdates(requireActivity())
            } else if (!PermissionsHandler.hasLocationPermissions(requireActivity())){
                PermissionsHandler.requestLocationPermissions(requireActivity())
                showDialogSettingsLocation()
                switchLocation.isChecked = false
                settingsViewModel.setBackgroundLocationDetection(requireContext(), false)

            } else {
                showDialogSettingsNotifications()
                switchLocation.isChecked = false
                settingsViewModel.setBackgroundLocationDetection(requireContext(), false)
            }

        } else {
            settingsViewModel.stopLocationUpdates(requireActivity())
            settingsViewModel.deregisterGeofence()
            settingsViewModel.removeGeofence()
        }
    }

    private fun showDialogSettingsLocation(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("You need permissions to detect locations!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.setData(uri)
                SETTINGS_PERMISSION_REQUEST_LOCATION.launch(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
        builder.show()
    }

    private fun showDialogSettingsNotifications(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("You need permissions to send notifications!")
            .setTitle("Permission required")
            .setCancelable(false)
            .setPositiveButton("Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.setData(uri)
                SETTINGS_PERMISSION_REQUEST_NOTIFICATION.launch(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
        builder.show()
    }


    private fun handleActivityRecognitionSwitch(isChecked: Boolean) {
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                if (PermissionsHandler.hasLocationPermissions(requireContext()) && PermissionsHandler.checkPermissionNotifications(requireActivity())) {
                    settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), true)
                    settingsViewModel.connectToActivityTransition(requireActivity())

                } else {
                    if (!PermissionsHandler.hasLocationPermissions(requireActivity())){
                        settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), false)
                        switchActivityRecognition.isChecked = false
                        showDialogSettingsLocation()
                    }

                    if (!PermissionsHandler.checkPermissionNotifications(requireActivity())){
                        settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), false)
                        switchActivityRecognition.isChecked = false
                        showDialogSettingsNotifications()
                    }
                }
            } else {
                settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), true)
                settingsViewModel.connectToActivityTransition(requireActivity())
            }

        }else {
            settingsViewModel.disconnect(requireActivity())
            settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), false)

        }
    }

    private fun showNumberOfStepsDialog() {
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)

        numberPicker.minValue = 1
        numberPicker.maxValue = (20000 - 200) / 200
        val displayValues = (200..20000 step 200).toList().map { it.toString() }.toTypedArray()
        numberPicker.displayedValues = displayValues

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
            .setTitle("Pick steps number")
            .setPositiveButton("OK") { _, _ ->
                val selectedNumber = displayValues[numberPicker.value - 1].toInt()

                scheduleStepsNotification(selectedNumber)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                switchStepsReminder.isChecked = false
            }.setCancelable(false)

        val alertDialog = builder.create()

        alertDialog.show()
    }

    private fun scheduleStepsNotification(selectedNumber: Int) {

        calculateDailySteps()
        settingsViewModel.scheduleStepsNotification(selectedNumber, requireContext())

        //Show to user
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

        val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val formattedTime = formatTime(selectedHour, selectedMinute)
            scheduleDailyNotification(selectedHour, selectedMinute, formattedTime)
        }, hour, minute, false) // false: to display in 12-hour format

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



    private fun goToFragmentMap(){
        val navController = findNavController()
        navController.navigate(R.id.mapFragment)

    }

    private fun goToFragmentHelp(){
        val navController = findNavController()
        navController.navigate(R.id.helpFragment)

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

    private fun checkPermissionsIfSettingsOn(){
        if (settingsViewModel.checkDailyReminder(requireActivity())){
            if (!PermissionsHandler.checkPermissionNotifications(requireActivity())){
                settingsViewModel.setDailyReminder(requireActivity(), false)
                switchDailyReminder.isChecked = false
                settingsViewModel.cancelDailyNotification(requireActivity())
            }
        }

        if (settingsViewModel.checkStepsReminder(requireActivity())){
            if (!PermissionsHandler.checkPermissionNotifications(requireActivity())){
                settingsViewModel.setStepsReminder(requireActivity(), false)
                switchStepsReminder.isChecked = false
                settingsViewModel.cancelStepsNotification(requireActivity())
            }
        }

        if (settingsViewModel.checkBackgroundRecogniseActivitiesOn(requireActivity())){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!PermissionsHandler.hasLocationPermissions(requireActivity()) || !PermissionsHandler.checkPermissionNotifications(
                        requireActivity()
                    )
                ) {
                    settingsViewModel.setBackgroundRecogniseActivities(requireActivity(), false)
                    switchActivityRecognition.isChecked = false
                    settingsViewModel.disconnect(requireActivity())
                }
            }
        }

        if (settingsViewModel.checkLocationSet(requireActivity())){
            if (!PermissionsHandler.hasLocationPermissions(requireActivity()) || !PermissionsHandler.checkPermissionNotifications(requireActivity())){
                settingsViewModel.setBackgroundLocationDetection(requireActivity(), false)
                switchLocation.isChecked = false
                settingsViewModel.stopLocationUpdates(requireActivity())
                settingsViewModel.deregisterGeofence()
                settingsViewModel.removeGeofence()
            }
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

