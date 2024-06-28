package com.example.personalphysicaltracker.ui.history

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentActivitiesDoneBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
class ActivitiesDoneFragment : Fragment() {

    private var _binding: FragmentActivitiesDoneBinding? = null
    private val binding get() = _binding!!

    private lateinit var activities: List<PhysicalActivity>
    private lateinit var calendarViewModel: CalendarViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityAdapter
    private lateinit var shareButton: ImageButton

    private lateinit var scrollView: ScrollView
    private lateinit var linearLayoutScrollView: LinearLayout
    private lateinit var chipGroup: ChipGroup



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activities_done, container, false)
        initializeViews(view)
        initializeViewModels()
        obtainDates()
        return view
    }


    private fun obtainDates() {
        activities = calendarViewModel.obtainActivitiesForTransaction()
        Log.d("ACTIVITIES OBTAINED", activities.size.toString())
        updateRecyclerView()

        // Populate ScrollView with activities
        //addInScrollBar()
    }

    private fun initializeViewModels() {
        // Initialize ViewModel using the Activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
    }


    // Initialize views from binding
    private fun initializeViews(view: View) {
        chipGroup = view.findViewById(R.id.chip_group)
        recyclerView = view.findViewById(R.id.recycler_view)
        shareButton = view.findViewById(R.id.button_share)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivityAdapter()
        recyclerView.adapter = adapter

        // Set chip click listeners
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener(chipClickListener)
        }

        shareButton.setOnClickListener{
            exportActivitiesToCSV()
        }
    }


    private fun updateRecyclerView() {
        adapter.submitList(activities)
    }


    private val chipClickListener = View.OnClickListener { view ->
        if (view is Chip) {
            val checked = view.isChecked
            val activityType = view.text.toString()
            if (checked) {
                activities = calendarViewModel.addFilter(activityType)
            } else {
                activities = calendarViewModel.removeFilter(activityType)
            }
            updateRecyclerView()
        }
    }


    private fun exportActivitiesToCSV() {
        val fileName = "activities.csv"
        val csvContent = generateCSVContent(activities) // Genera il contenuto CSV

        val csvUri = saveCSVFileToMediaStore(requireContext(), fileName, csvContent) // Salva il file CSV nel MediaStore
        csvUri?.let {
            shareCSVFile(it) // Condividi il file CSV
        }
    }

    private fun generateCSVContent(activities: List<PhysicalActivity>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Activity,Date,Started At,Ended At,Duration,Steps\n")
        activities.forEach { activity ->
            val startTime = activity.start.substring(11)
            val endTime = activity.end.substring(11)
            val steps = if (activity.getActivityTypeName() == ActivityType.WALKING) {
                (activity as WalkingActivity).getSteps().toString()
            } else {
                ""
            }
            stringBuilder.append("${activity.getActivityTypeName()},${activity.date},$startTime,$endTime,${activity.duration},$steps\n")
        }
        return stringBuilder.toString()
    }

    private fun saveCSVFileToMediaStore(context: Context, fileName: String, csvContent: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS) // Imposta il percorso relativo
            }
        }

        var csvUri: Uri? = null
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            csvUri = contentResolver.insert(contentUri, contentValues) // Inserisci il file nel MediaStore

            csvUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray()) // Scrivi il contenuto nel file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return csvUri
    }

    private fun shareCSVFile(csvUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, csvUri) // Aggiungi l'URI del file CSV
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share CSV")) // Avvia l'intenzione di condivisione
    }

    override fun onDestroyView() {
        super.onDestroyView()
        calendarViewModel.clearFilters()
        _binding = null
    }


}
