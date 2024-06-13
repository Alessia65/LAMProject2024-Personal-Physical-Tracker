package com.example.personalphysicaltracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AlertDialog
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.AccelerometerListener
import com.example.personalphysicaltracker.activities.Activity
import com.example.personalphysicaltracker.activities.DrivingActivity
import com.example.personalphysicaltracker.activities.StandingActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), AccelerometerListener {

    // Binding per il Fragment
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel per gestire i dati del Fragment
    private lateinit var homeViewModel: HomeViewModel

    // View del Fragment
    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button
    private lateinit var accelText: TextView // Dichiarazione della TextView per mostrare i dati dell'accelerometro

    // Variabile per memorizzare l'attività selezionata
    private var selectedActivity: Activity? = null

    // Metodo chiamato quando il Fragment viene creato o ricreato
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del Fragment utilizzando il binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // Inizializza il ViewModel associato a questo Fragment
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Ottiene riferimenti alle view dal layout del Fragment
        textView = binding.textHome
        buttonStartActivity = root.findViewById(R.id.button_startActivity)
        accelText = root.findViewById(R.id.acceleration_text) // Assumi che ci sia un TextView con id acceleration_text nel layout

        // Imposta il listener per il pulsante "Start activity"
        buttonStartActivity.setOnClickListener {
            // Mostra un dialogo per la selezione dell'attività
            showActivitySelectionDialog()
        }

        // Restituisce la vista radice del Fragment
        return root
    }

    // Mostra un dialogo che permette all'utente di selezionare un'attività
    private fun showActivitySelectionDialog() {
        val items = arrayOf("Walking", "Driving", "Standing")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Seleziona un'attività:")
            .setItems(items) { dialog, item ->
                // Avvia l'attività corrispondente in base alla selezione
                when (items[item]) {
                    "Walking" -> startWalkingActivity()
                    "Driving" -> startDrivingActivity()
                    "Standing" -> startStandingActivity()
                }
            }
        val alert = builder.create()
        alert.show()
    }

    // Avvia l'Activity di camminata
    private fun startWalkingActivity() {
        selectedActivity = WalkingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor() // Avvia la gestione del sensore per WalkingActivity
        changeButton()
    }

    // Avvia l'Activity di guida
    private fun startDrivingActivity() {
        selectedActivity = DrivingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor()
        changeButton()
    }

    // Avvia l'Activity di stazionamento
    private fun startStandingActivity() {
        selectedActivity = StandingActivity(requireContext())
        selectedActivity?.registerAccelerometerListener(this)
        selectedActivity?.startSensor()
        changeButton()
    }

    // Cambia il testo e il comportamento del pulsante in "Stop Activity"
    private fun changeButton() {
        buttonStartActivity.text = "Stop Activity"
        buttonStartActivity.setOnClickListener {
            stopSelectedActivity()
        }
    }

    // Ferma l'attività selezionata
    private fun stopSelectedActivity() {
        selectedActivity?.stopActivity()
        buttonStartActivity.text = "Start Activity"
        buttonStartActivity.setOnClickListener {
            showActivitySelectionDialog()
        }
        requireActivity().runOnUiThread {
            accelText.text = "No activity running"
        }
    }

    // Implementazione del metodo dell'interfaccia AccelerometerDataListener
    override fun onAccelerometerDataReceived(data: String) {
        // Aggiorna l'UI con i dati dell'accelerometro ricevuti
        requireActivity().runOnUiThread {
            accelText.text = data
        }
    }

    // Metodo chiamato quando il Fragment viene distrutto per liberare le risorse associate
    override fun onDestroyView() {
        super.onDestroyView()
        selectedActivity?.stopActivity() // Arresta l'attività selezionata quando il Fragment viene distrutto
        _binding = null
    }
}
