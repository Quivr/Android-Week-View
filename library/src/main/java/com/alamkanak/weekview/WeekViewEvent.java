package com.alamkanak.weekview;

import android.graphics.Shader;
import android.support.annotation.ColorInt;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.alamkanak.weekview.WeekViewUtil.isSameDay;

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://april-shower.com
 */
public class WeekViewEvent {
    private String mId;
    private LocalDateTime mStartTime;
    private LocalDateTime mEndTime;
    private String mName;
    private String mLocation;
    private @ColorInt int mColor;
    private boolean mAllDay;
    private Shader mShader;

    public WeekViewEvent() {

    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event as String.
     * @param name Name of the event.
     * @param startYear Year when the event starts.
     * @param startMonth Month when the event starts.
     * @param startDay Day when the event starts.
     * @param startHour Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear Year when the event ends.
     * @param endMonth Month when the event ends.
     * @param endDay Day when the event ends.
     * @param endHour Hour (in 24-hour format) when the event ends.
     * @param endMinute Minute when the event ends.
     */
    public WeekViewEvent(String id, String name, int startYear, int startMonth, int startDay, int startHour, int
            startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute) {
        this.mId = id;

        this.mStartTime = LocalDateTime.of(startYear, startMonth, startDay, startHour, startMinute);
        this.mEndTime = LocalDateTime.of(endYear, endMonth, endDay, endHour, endMinute);

        this.mName = name;
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startYear Year when the event starts.
     * @param startMonth Month when the event starts.
     * @param startDay Day when the event starts.
     * @param startHour Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear Year when the event ends.
     * @param endMonth Month when the event ends.
     * @param endDay Day when the event ends.
     * @param endHour Hour (in 24-hour format) when the event ends.
     * @param endMinute Minute when the event ends.
     */
    @Deprecated
    public WeekViewEvent(long id, String name, int startYear, int startMonth, int startDay, int startHour, int
            startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute) {
        this(String.valueOf(id), name, startYear, startMonth, startDay, startHour, startMinute, endYear, endMonth,
                endDay, endHour, endMinute);
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event as String.
     * @param name Name of the event.
     * @param location The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     * @param allDay Is the event an all day event.
     * @param shader the Shader of the event rectangle
     */
    public WeekViewEvent(String id, String name, String location, LocalDateTime startTime, LocalDateTime endTime, boolean allDay, Shader shader) {
        this.mId = id;
        this.mName = name;
        this.mLocation = location;
        this.mStartTime = startTime;
        this.mEndTime = endTime;
        this.mAllDay = allDay;
        this.mShader = shader;
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event.
     * @param name Name of the event.
     * @param location The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     * @param allDay Is the event an all day event.
     * @param shader the Shader of the event rectangle
     */
    @Deprecated
    public WeekViewEvent(long id, String name, String location, LocalDateTime startTime, LocalDateTime endTime, boolean allDay,
            Shader shader) {
        this(String.valueOf(id), name, location, startTime, endTime, allDay, shader);
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event as String.
     * @param name Name of the event.
     * @param location The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     * @param allDay Is the event an all day event
     */
    public WeekViewEvent(String id, String name, String location, LocalDateTime startTime, LocalDateTime endTime, boolean
            allDay) {
        this(id, name, location, startTime, endTime, allDay, null);
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event as String.
     * @param name Name of the event.
     * @param location The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
    public WeekViewEvent(String id, String name, String location, LocalDateTime startTime, LocalDateTime endTime) {
        this(id, name, location, startTime, endTime, false);
    }

    /**
     * Initializes the event for week view.
     *
     * @param id The id of the event specified as String.
     * @param name Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
    public WeekViewEvent(String id, String name, LocalDateTime startTime, LocalDateTime endTime) {
        this(id, name, null, startTime, endTime);
    }

    public LocalDateTime getStartTime() {
        return mStartTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.mStartTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return mEndTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.mEndTime = endTime;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        this.mLocation = location;
    }

    public @ColorInt int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public boolean isAllDay() {
        return mAllDay;
    }

    public void setAllDay(boolean allDay) {
        this.mAllDay = allDay;
    }

    public Shader getShader() {
        return mShader;
    }

    public void setShader(Shader shader) {
        mShader = shader;
    }

    public String getIdentifier() {
        return mId;
    }

    public void setIdentifier(String id) {
        this.mId = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        WeekViewEvent that = (WeekViewEvent) o;

        return mId.equals(that.mId);
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    public List<WeekViewEvent> splitWeekViewEvents() {
        //This function splits the WeekViewEvent in WeekViewEvents by day
        List<WeekViewEvent> events = new ArrayList<>();
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        LocalDateTime endTime = this.mEndTime.minusNanos(1);
        if (!isSameDay(this.getStartTime(), endTime)) {
            endTime = this.mStartTime.with(LocalTime.MAX);
            WeekViewEvent event1 = new WeekViewEvent(this.mId, this.mName, this.mLocation, this.mStartTime, endTime, this.mAllDay);
            event1.setColor(this.mColor);
            events.add(event1);

            // Add other days.
            LocalDateTime otherDay = this.mStartTime.withDayOfMonth(1);
            while (!isSameDay(otherDay, this.getEndTime())) {
                LocalDateTime overDay = otherDay.with(LocalTime.MIN);
                LocalDateTime endOfOverDay = overDay.with(LocalTime.MAX);
                WeekViewEvent eventMore = new WeekViewEvent(this.mId, this.mName, null, overDay, endOfOverDay, this.mAllDay);
                eventMore.setColor(this.getColor());
                events.add(eventMore);

                // Add next day.
                otherDay = otherDay.plusDays(1);
            }

            // Add last day.
            LocalDateTime startTime = this.mEndTime.with(LocalTime.MIN);
            WeekViewEvent event2 = new WeekViewEvent(this.mId, this.mName, this.mLocation, startTime, this.mEndTime, this.mAllDay);
            event2.setColor(this.getColor());
            events.add(event2);
        } else {
            events.add(this);
        }

        return events;
    }
}
