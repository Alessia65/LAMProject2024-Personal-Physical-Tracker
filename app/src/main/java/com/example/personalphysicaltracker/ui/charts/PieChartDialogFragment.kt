package com.example.personalphysicaltracker.ui.charts

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.databinding.DialogPieChartBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class PieChartDialogFragment : DialogFragment() {

    private var _binding: DialogPieChartBinding? = null
    private val binding get() = _binding!!

    private lateinit var pieChart: PieChart
    private lateinit var chartsViewModel: ChartsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPieChartBinding.inflate(inflater, container, false)
        val view = binding.root

        chartsViewModel = ViewModelProvider(requireActivity()).get(ChartsViewModel::class.java)

        pieChart = binding.dialogPieChart
        setupPieChart()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Imposta il titolo del dialog (opzionale)
        dialog?.setTitle("Pie Chart Dialog")

        // Imposta le dimensioni del dialog (opzionale)
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)
    }

    private fun setupPieChart() {
        // Popola il PieChart con i dati desiderati
        val entries = ArrayList<PieEntry>()
        val dates = chartsViewModel.obtainDatesForDialog()

        entries.add(PieEntry(dates[0], "Walking"))
        entries.add(PieEntry(dates[1], "Driving"))
        entries.add(PieEntry(dates[2], "Standing"))
        entries.add(PieEntry(dates[3], "Nothing"))

        val dataSet = PieDataSet(entries, "Activities")
        dataSet.colors = listOf(Color.GREEN, Color.BLUE, Color.RED, Color.GRAY)
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.invalidate()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
