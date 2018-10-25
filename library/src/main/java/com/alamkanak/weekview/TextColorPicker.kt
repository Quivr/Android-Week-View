package com.alamkanak.weekview

import androidx.annotation.ColorInt

interface TextColorPicker {

    @ColorInt
    fun getTextColor(event: WeekViewEvent): Int

}
