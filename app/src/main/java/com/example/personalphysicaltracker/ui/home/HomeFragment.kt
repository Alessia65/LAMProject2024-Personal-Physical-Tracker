package com.example.personalphysicaltracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AlertDialog
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    // Binding per il Fragment
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel per gestire i dati del Fragment
    private lateinit var homeViewModel: HomeViewModel

    // View del Fragment
    private lateinit var textView: TextView
    private lateinit var buttonStartActivity: Button

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

        // Osserva i dati nel ViewModel e aggiorna l'interfaccia utente di conseguenza
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

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
        val items = arrayOf("Camminata", "Guida", "Fermo")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleziona un'attività:")
            .setItems(items) { dialog, item ->
                // Mostra un Toast con l'attività selezionata dall'utente
                showToast(items[item])
            }
        val alert = builder.create()
        alert.show()
    }

    // Mostra un Toast con il messaggio specificato
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    // Metodo chiamato quando il Fragment viene distrutto per liberare le risorse associate
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
