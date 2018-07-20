package com.alamkanak.weekview.sample;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import com.alamkanak.weekview.TextColorPicker;
import com.alamkanak.weekview.WeekViewEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A basic example of how to use week view library.
 * Created by Raquib-ul-Alam Kanak on 1/3/2014.
 * Website: http://alamkanak.github.io
 */
public class BasicActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Typeface customTypeface = Typeface.createFromAsset(this.getAssets(), "fonts/Raleway/Raleway-Medium.ttf");
        mWeekView.setTypeface(customTypeface);
        mWeekView.setTextColorPicker(new TextColorPicker() {
            @Override
            public int getTextColor(WeekViewEvent event) {
                int color = event.getColor();
                double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
                return a < 0.2 ? Color.BLACK : Color.WHITE;
            }
        });
    }

    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        // Populate the week view with some events.
        List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            LocalDateTime startTime = LocalDateTime.now().plusHours(i*(random.nextInt(3) + 1));
            LocalDateTime endTime = startTime.plusMinutes(30);
            events.add(new WeekViewEvent("ID" + i, "Event " + i, startTime, endTime));
        }
        return events;
    }
}
