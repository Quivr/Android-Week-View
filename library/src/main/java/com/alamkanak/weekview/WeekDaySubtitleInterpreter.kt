package com.alamkanak.weekview

import java.util.*

interface WeekDaySubtitleInterpreter {
    fun getFormattedWeekDaySubtitle(date: Calendar): String
}
