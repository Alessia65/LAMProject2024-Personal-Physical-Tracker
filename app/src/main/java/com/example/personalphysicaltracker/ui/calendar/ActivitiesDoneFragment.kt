package com.example.personalphysicaltracker.ui.calendar

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayoutScrollView: LinearLayout
    private lateinit var chipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentActivitiesDoneBinding.inflate(inflater, container, false)

        initializeViews()
        initializeViewModels()
        obtainDates()




        return binding.root
    }

    private fun obtainDates() {
        activities = calendarViewModel.obtainActivitiesForTransaction()
        Log.d("ACTIVITIES OBTAINED", activities.size.toString())

        // Populate ScrollView with activities
        addInScrollBar()
    }

    private fun initializeViewModels() {
        // Initialize ViewModel using the Activity as ViewModelStoreOwner
        calendarViewModel = ViewModelProvider(requireActivity()).get(CalendarViewModel::class.java)
    }


    // Initialize views from binding
    private fun initializeViews(){
        scrollView = binding.root.findViewById(R.id.scroll_view)
        linearLayoutScrollView = binding.root.findViewById(R.id.linear_layout_scroll)

        chipGroup = binding.root.findViewById(R.id.chip_group)
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Aggiungi il filtro selezionato
                    activities = calendarViewModel.addFilter(chip.text.toString())
                } else {
                    // Rimuovi il filtro selezionato
                    activities = calendarViewModel.removeFilter(chip.text.toString())
                }
                // Popola ScrollView con le attività filtrate
                addInScrollBar()
            }
        }


    }



    private fun addInScrollBar() {
        // Clear previous views in the ScrollView
        binding.scrollView.removeAllViews()
        binding.linearLayoutScroll.removeAllViews()

        if (activities.isNotEmpty()) {
            // Add each activity to the ScrollView
            for (activity in activities) {
                addInScrollView(activity)
            }

            // Add linearLayoutScrollView to the ScrollView
            binding.scrollView.addView(linearLayoutScrollView)
        }
    }

    //TODO: aggiungere passi
    private fun addInScrollView(activity: PhysicalActivity) {
        val startTime = activity.start
        val endTime = activity.end

        val builder = SpannableStringBuilder()

        // Aggiungi "Activity: " con stile grassetto
        builder.append(getSpannableString("Activity: ", activity.getActivityTypeName().toString()))
        // Aggiungi "Date: " con stile grassetto
        builder.append(getSpannableString("Date: ", activity.date))
        // Aggiungi "Started at: " con stile grassetto
        builder.append(getSpannableString("Started at: ", startTime))
        // Aggiungi "Ended at: " con stile grassetto
        builder.append(getSpannableString("Ended at: ", endTime))
        // Aggiungi "Duration: " con stile grassetto
        builder.append(getSpannableString("Duration: ", activity.duration.toString()))

        if(activity.getActivityTypeName() == ActivityType.WALKING){
            builder.append(getSpannableString("Steps: ", (activity as WalkingActivity).getSteps().toString()))
        }
        val textView = TextView(requireContext())
        textView.text = builder

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = 16
        textView.layoutParams = layoutParams

        binding.linearLayoutScroll.addView(textView)
    }

    private fun getSpannableString(prefix: String, value: String): SpannableStringBuilder {
        val spannableString = SpannableStringBuilder()
        spannableString.append(prefix)
        val start = spannableString.length
        spannableString.append(value)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.append("\n")
        return spannableString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        calendarViewModel.clearFilters()
        _binding = null
    }


}
