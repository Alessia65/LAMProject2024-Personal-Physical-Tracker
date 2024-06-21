package com.example.personalphysicaltracker.ui.graphics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.WalkingActivity
import com.example.personalphysicaltracker.databinding.FragmentGraphicsWalkingBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.datatype.DatatypeConstants.MONTHS


class WalkingGraphicsFragment : Fragment() {

    private var _binding: FragmentGraphicsWalkingBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectDay: TextView
    private lateinit var textRange: TextView

    private lateinit var graphicsViewModel: GraphicsViewModel
    private var walkingActivitiesToShow: List<WalkingActivity> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGraphicsWalkingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        graphicsViewModel = ViewModelProvider(requireActivity()).get(GraphicsViewModel::class.java)


        initializeViews()
        return root
    }

    fun initializeViews(){
        textRange = binding.root.findViewById(R.id.text_range)
        selectDay = binding.root.findViewById(R.id.select_day)
        selectDay.setOnClickListener {
            showDatePickerDialog("DAY")
        }
    }

    private fun showDatePickerDialog(type: String) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeCalendar)
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.show(requireActivity().supportFragmentManager, "DATE_PICKER")

        picker.addOnPositiveButtonClickListener {
            val startSelectedDate = Date(it)
            val startFormattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startSelectedDate)

            var endDate = ""

            if (type.equals("DAY")){
                endDate = startFormattedDate
                textRange.text = startFormattedDate
            } else if (type.equals("WEEK")){
                // endDate = a 7 giorni dopo
            }  else if (type.equals("MONTH")){
                // endDate = a 30 giorni dopo
            } else if (type.equals("YEAR")){
                // endDate = a 365 giorni dopo
            }

            if (startFormattedDate != null && endDate != "") {

                //Log.d("DATE_RANGE_SELECTED", "$formattedStartDate - $formattedEndDate")

                walkingActivitiesToShow = graphicsViewModel.handleSelectedDateRangeWalking(startFormattedDate, endDate)

                for (w in walkingActivitiesToShow){
                    Log.d("WA", w.getActivityTypeName().toString() + " , " + w.date + " , " + w.duration + " , " + w.getSteps())
                }
            }
        }

        picker.addOnNegativeButtonClickListener {
            picker.dismiss()
        }
    }

    /*
    private fun convertTimeToDate(time: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = time
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(utc.time)
    }

     */


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
