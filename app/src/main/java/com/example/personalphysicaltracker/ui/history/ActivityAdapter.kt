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
import android.util.Log


class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    private var activities = listOf<PhysicalActivity>()

    fun submitList(list: List<PhysicalActivity>) {
        activities = list
        notifyDataSetChanged()
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

        private val textViewActivityDetailsActivityType: TextView = itemView.findViewById(R.id.textViewActivityDetailsActivityType)
        private val textViewActivityDetailsDate: TextView = itemView.findViewById(R.id.textViewActivityDetailsDate)
        private val textViewActivityDetailsStartedAt: TextView = itemView.findViewById(R.id.textViewActivityDetailsStartedAt)
        private val textViewActivityDetailsEndedAt: TextView = itemView.findViewById(R.id.textViewActivityDetailsEndedAt)
        private val textViewActivityDetailsDuration: TextView = itemView.findViewById(R.id.textViewActivityDetailsDuration)
        private val textViewActivityDetailsSteps: TextView = itemView.findViewById(R.id.textViewActivityDetailsSteps)

        fun bind(activity: PhysicalActivity) {
            val startTime = activity.start.substring(11)

            val endTime = activity.end.substring(11)

            val builderActivityType = SpannableStringBuilder()
            //builderActivityType.append(getSpannableString("Activity", activity.getActivityTypeName().toString()))

            builderActivityType.append(activity.getActivityTypeName().toString())


            val builderDate = SpannableStringBuilder()
            builderDate.append(getSpannableString("Date", activity.date))

            val builderStartedAt = SpannableStringBuilder()
            builderStartedAt.append(getSpannableString("Started at", startTime))

            val builderEndedAt = SpannableStringBuilder()
            builderEndedAt.append(getSpannableString("Ended at", endTime))

            val builderDuration= SpannableStringBuilder()
            builderDuration.append(getSpannableString("Duration", activity.duration.toString()))

            textViewActivityDetailsActivityType.text = builderActivityType
            textViewActivityDetailsDate.text = builderDate
            textViewActivityDetailsStartedAt.text = builderStartedAt
            textViewActivityDetailsEndedAt.text = builderEndedAt
            textViewActivityDetailsDuration.text = builderDuration

            if (activity.getActivityTypeName() == ActivityType.WALKING) {
                val builderSteps = SpannableStringBuilder()
                builderSteps.append(getSpannableString("Steps", (activity as WalkingActivity).getSteps().toString()))
                textViewActivityDetailsSteps.text = builderSteps
                textViewActivityDetailsSteps.visibility = View.VISIBLE
            } else {
                textViewActivityDetailsSteps.visibility = View.GONE
            }
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
