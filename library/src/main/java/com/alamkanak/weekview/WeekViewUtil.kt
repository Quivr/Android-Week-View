package com.alamkanak.weekview

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object WeekViewUtil {
    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Checks if two dates are on the same day.
     *
     * @param dateOne The first date.
     * @param dateTwo The second date.     *
     * @return Whether the dates are on the same day.
     */
    @JvmStatic
    fun isSameDay(dateOne: Calendar, dateTwo: Calendar): Boolean {
        if (dateOne === dateTwo)
            return true
        return dateOne.get(Calendar.YEAR) == dateTwo.get(Calendar.YEAR) && dateOne.get(Calendar.DAY_OF_YEAR) == dateTwo.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Returns a calendar instance at the start of today
     *
     * @return the calendar instance
     */
    @JvmStatic
    fun today(): Calendar {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }

    @JvmStatic
    fun isSameDayAndHourAndMinute(dateOne: Calendar, dateTwo: Calendar): Boolean {
        return isSameDay(dateOne, dateTwo) && dateOne.get(Calendar.HOUR_OF_DAY) == dateTwo.get(Calendar.HOUR_OF_DAY)
                && dateOne.get(Calendar.MINUTE) == dateTwo.get(Calendar.MINUTE)
    }

    /**
     * Returns the amount of days between the second date and the first date
     *
     * @param dateOne the first date
     * @param dateTwo the second date
     * @return the amount of days between dateTwo and dateOne
     */
    @JvmStatic
    fun daysBetween(dateOne: Calendar, dateTwo: Calendar): Int {
        return ((dateTwo.timeInMillis + dateTwo.timeZone.getOffset(dateTwo.timeInMillis)) / (1000 * 60 * 60 * 24) - (dateOne.timeInMillis + dateOne.timeZone.getOffset(dateOne.timeInMillis)) / (1000 * 60 * 60 * 24)).toInt()
    }

    /*
    * Returns the amount of minutes passed in the day before the time in the given date
    * @param date
    * @return amount of minutes in day before time
    */
    @JvmStatic
    fun getPassedMinutesInDay(date: Calendar): Int {
        return getPassedMinutesInDay(date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE))
    }

    /**
     * Returns the amount of minutes in the given hours and minutes
     *
     * @param hour
     * @param minute
     * @return amount of minutes in the given hours and minutes
     */
    @JvmStatic
    fun getPassedMinutesInDay(hour: Int, minute: Int): Int {
        return hour * 60 + minute
    }

    /**returns a date format of dayOfWeek+day&month, based on the current locale.
     * This is important, as the format is different in many countries. Especially the numeric part that can be different : "d/M", "M/d", "d-M", "M-d" ,...*/
    @JvmStatic
    fun getWeekdayWithNumericDayAndMonthFormat(context: Context, shortDate: Boolean): java.text.DateFormat {
        val weekDayFormat = if (shortDate) "EEEEE" else "EEE"
        val defaultDateFormatPattern = "$weekDayFormat d/M"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val locale = Locale.getDefault()
            var bestDateTimePattern = DateFormat.getBestDateTimePattern(locale, defaultDateFormatPattern)
            //workaround fix for this issue: https://issuetracker.google.com/issues/79311044
            //TODO if there is a better API that doesn't require this workaround, use it. Be sure to check vs all locales, as done here: https://issuetracker.google.com/issues/37044127
            bestDateTimePattern = bestDateTimePattern.replace("d+".toRegex(), "d").replace("M+".toRegex(), "M")
            bestDateTimePattern = bestDateTimePattern.replace("E+".toRegex(), weekDayFormat)
            return SimpleDateFormat(bestDateTimePattern, locale)
        }
        try {
            val dateFormatOrder = DateFormat.getDateFormatOrder(context)
            if (dateFormatOrder.isEmpty())
                return SimpleDateFormat(defaultDateFormatPattern, Locale.getDefault())
            val sb = StringBuilder()
            for (i in dateFormatOrder.indices) {
                val c = dateFormatOrder[i]
                if (Character.toLowerCase(c) == 'y')
                    continue
                if (sb.isNotEmpty())
                    sb.append('/')
                when (Character.toLowerCase(c)) {
                    'm' -> sb.append("M")
                    'd' -> sb.append("d")
                }
            }
            val dateFormatString = sb.toString()
            return SimpleDateFormat("$weekDayFormat $dateFormatString", Locale.getDefault())
        } catch (e: Exception) {
            return SimpleDateFormat(defaultDateFormatPattern, Locale.getDefault())
        }
    }

    @JvmStatic
    fun calendarToString(cal: Calendar?, includeTime: Boolean = true): String {
        if (cal == null)
            return ""
        val sb = StringBuilder()
        with(cal) {
            sb.append(get(Calendar.YEAR).toString()).append('-').append((get(Calendar.MONTH) + 1).toString())
                    .append('-').append(get(Calendar.DAY_OF_MONTH).toString())
            if (includeTime)
                sb.append(" ").append(get(Calendar.HOUR_OF_DAY).toString()).append(':').append(get(Calendar.MINUTE).toString()).append(':')
                        .append(get(Calendar.SECOND).toString()).append('.').append(get(Calendar.MILLISECOND).toString())
        }
        return sb.toString()
    }

    @JvmStatic
    fun resetTime(cal: Calendar) {
        with(cal)
        {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }
}
