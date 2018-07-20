package com.alamkanak.weekview.sample.apiclient;

import android.annotation.SuppressLint;
import android.graphics.Color;

import com.alamkanak.weekview.WeekViewEvent;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * An event model that was built for automatic serialization from json to object.
 * Created by Raquib-ul-Alam Kanak on 1/3/16.
 * Website: http://alamkanak.github.io
 */
public class Event {

    @Expose
    @SerializedName("name")
    private String mName;
    @Expose
    @SerializedName("dayOfMonth")
    private int mDayOfMonth;
    @Expose
    @SerializedName("startTime")
    private String mStartTime;
    @Expose
    @SerializedName("endTime")
    private String mEndTime;
    @Expose
    @SerializedName("color")
    private String mColor;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getDayOfMonth() {
        return mDayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.mDayOfMonth = dayOfMonth;
    }

    public String getStartTime() {
        return mStartTime;
    }

    public void setStartTime(String startTime) {
        this.mStartTime = startTime;
    }

    public String getEndTime() {
        return mEndTime;
    }

    public void setEndTime(String endTime) {
        this.mEndTime = endTime;
    }

    public String getColor() {
        return mColor;
    }

    public void setColor(String color) {
        this.mColor = color;
    }

    @SuppressLint("SimpleDateFormat")
    public WeekViewEvent toWeekViewEvent() {

        // Parse time.
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date start = new Date();
        Date end = new Date();
        try {
            start = sdf.parse(getStartTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            end = sdf.parse(getEndTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Initialize start and end time.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime after = now.plusHours(1);

        // Create an week view event.
        WeekViewEvent weekViewEvent = new WeekViewEvent();
        weekViewEvent.setIdentifier(getName());
        weekViewEvent.setName(getName());
        weekViewEvent.setStartTime(now);
        weekViewEvent.setEndTime(after);
        weekViewEvent.setColor(Color.parseColor(getColor()));

        return weekViewEvent;
    }
}
