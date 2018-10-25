package com.alamkanak.weekview.sample.apiclient

import android.annotation.SuppressLint
import android.graphics.Color
import com.alamkanak.weekview.WeekViewEvent
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * An event model that was built for automatic serialization from json to object.
 */
class Event {

    @Expose
    @SerializedName("name")
    var name: String? = null
    @Expose
    @SerializedName("dayOfMonth")
    var dayOfMonth: Int = 0
    @Expose
    @SerializedName("startTime")
    var startTime: String? = null
    @Expose
    @SerializedName("endTime")
    var endTime: String? = null
    @Expose
    @SerializedName("color")
    var color: String? = null

    @SuppressLint("SimpleDateFormat")
    fun toWeekViewEvent(): WeekViewEvent {

        // Parse time.
        val sdf = SimpleDateFormat("HH:mm")
        var start = Date()
        var end = Date()
        try {
            start = sdf.parse(startTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        try {
            end = sdf.parse(endTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        // Initialize start and end time.
        val now = Calendar.getInstance()
        val startTime = Calendar.getInstance()
        startTime.timeInMillis = start.time
        with(startTime) {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        val endTime = startTime.clone() as Calendar
        endTime.timeInMillis = end.time
        with(endTime) {
            set(Calendar.YEAR, startTime.get(Calendar.YEAR))
            set(Calendar.MONTH, startTime.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, startTime.get(Calendar.DAY_OF_MONTH))
        }

        // Create an week view event.
        val weekViewEvent = WeekViewEvent(name, name, null, startTime, endTime)
        weekViewEvent.color = Color.parseColor(color)

        return weekViewEvent
    }
}
