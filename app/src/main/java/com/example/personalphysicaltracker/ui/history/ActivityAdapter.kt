package com.example.personalphysicaltracker.ui.history

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalphysicaltracker.R
import com.example.personalphysicaltracker.activities.ActivityType
import com.example.personalphysicaltracker.activities.PhysicalActivity
import com.example.personalphysicaltracker.activities.WalkingActivity
import android.graphics.Typeface


class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    private var activities = listOf<PhysicalActivity>()

    fun submitList(list: List<PhysicalActivity>) {
        activities = list
        notifyDataSetChanged()  // Assicurati che notifyDataSetChanged() venga chiamato correttamente qui
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])  // Assicurati che bind() imposti correttamente i dati nella ViewHolder
    }

    override fun getItemCount(): Int {
        return activities.size
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewActivityDetails: TextView = itemView.findViewById(R.id.textViewActivityDetails)

        fun bind(activity: PhysicalActivity) {
            val startTime = activity.start.substring(11)
            val endTime = activity.end.substring(11)

            val builder = SpannableStringBuilder()
            builder.append(getSpannableString("Activity", activity.getActivityTypeName().toString()))
            builder.append(getSpannableString("Date", activity.date))
            builder.append(getSpannableString("Started at", startTime))
            builder.append(getSpannableString("Ended at", endTime))
            builder.append(getSpannableString("Duration", activity.duration.toString()))

            if (activity.getActivityTypeName() == ActivityType.WALKING) {
                builder.append(getSpannableString("Steps", (activity as WalkingActivity).getSteps().toString()))
            }

            textViewActivityDetails.text = builder
        }

        private fun getSpannableString(prefix: String, value: String): SpannableStringBuilder {
            val spannableString = SpannableStringBuilder()
            spannableString.append("$prefix: ")
            val start = spannableString.length
            spannableString.append(value)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, start - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.append("\n")
            return spannableString
        }
    }



}
