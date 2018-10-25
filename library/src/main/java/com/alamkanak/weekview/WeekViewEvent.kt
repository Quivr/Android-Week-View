package com.alamkanak.weekview

import android.graphics.Shader
import androidx.annotation.ColorInt
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import java.util.*

open class WeekViewEvent {
    val id: String?
    val startTime: Calendar
    val endTime: Calendar
    var name: String? = null
    var location: String? = null
    @ColorInt
    @get:ColorInt
    var color: Int = 0
    val isAllDay: Boolean
    var shader: Shader? = null

    /**CTOR for a single, all day event*/
    constructor(id: String?, name: String?, location: String? = null, allDayTime: Calendar, shader: Shader? = null) : this(id, name, location, allDayTime, allDayTime, true, shader)

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event as String.
     * @param name      Name of the event.
     * @param location  The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     * @param allDay    Is the event an all day event.
     * @param shader    the Shader of the event rectangle
     */
    @JvmOverloads constructor(id: String?, name: String?, location: String?, startTime: Calendar, endTime: Calendar, allDay: Boolean = false, shader: Shader? = null) {
        this.id = id
        this.name = name
        this.location = location
        this.isAllDay = allDay
        if (!allDay) {
            this.startTime = startTime
            this.endTime = endTime
        } else {
            WeekViewUtil.resetTime(startTime)
            this.startTime = startTime
            if (!WeekViewUtil.isSameDay(startTime, endTime)) {
                WeekViewUtil.resetTime(endTime)
                this.endTime = endTime
            } else
                this.endTime = startTime
        }
        this.shader = shader
    }

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event specified as String.
     * @param name      Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     */
    constructor(id: String?, name: String?, startTime: Calendar, endTime: Calendar) : this(id, name, null, startTime, endTime)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeekViewEvent) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    fun splitWeekViewEvents(): MutableList<WeekViewEvent> {
        //This function splits the WeekViewEvent in WeekViewEvents by day
        if (isSameDay(this.startTime, this.endTime)) {
            val events = ArrayList<WeekViewEvent>(1)
            events.add(this)
            return events
        }
        val events = ArrayList<WeekViewEvent>()
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        var endTime = this.endTime.clone() as Calendar
        endTime.add(Calendar.MILLISECOND, -1)
        endTime = this.startTime.clone() as Calendar
        endTime.set(Calendar.HOUR_OF_DAY, 23)
        endTime.set(Calendar.MINUTE, 59)
        val event1 = WeekViewEvent(this.id, this.name, this.location, this.startTime, endTime, this.isAllDay)
        event1.color = this.color
        events.add(event1)
        // Add other days.
        if (!isSameDay(this.startTime, this.endTime)) {
            val otherDay = this.startTime.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!isSameDay(otherDay, this.endTime)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(this.id, this.name, null, overDay, endOfOverDay, this.isAllDay)
                eventMore.color = this.color
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }
            // Add last day.
            val startTime = this.endTime.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(this.id, this.name, this.location, startTime, this.endTime, this.isAllDay)
            event2.color = this.color
            events.add(event2)
        }
        return events
    }

    override fun toString(): String {
        val colorStr = "#${Integer.toHexString(color)}"
        val startTimeStr = WeekViewUtil.calendarToString(startTime, !isAllDay)
        if (isAllDay) {
            if (WeekViewUtil.isSameDay(startTime, endTime))
                return "allDayEvent(id=$id, time=$startTimeStr..${WeekViewUtil.calendarToString(startTime, false)}, name=$name, location=$location, color=$colorStr ,shader=$shader)"
            return "allDayEvent(id=$id, time=$startTimeStr, name=$name, location=$location, color=$colorStr ,shader=$shader)"
        }
        val endTimeStr = WeekViewUtil.calendarToString(endTime, true)
        return "normalEvent(id=$id, startTime=$colorStr, endTime=$endTimeStr, name=$name, location=$location, color=$colorStr , shader=$shader)"
    }
}
