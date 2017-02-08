package com.alamkanak.weekview;

import android.support.annotation.ColorInt;

/**
 * Created by Wietse Buseyne on 8-2-2017.
 */
public interface TextColorPicker {

    @ColorInt int getTextColor(WeekViewEvent event);

}
