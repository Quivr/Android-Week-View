package com.alamkanak.weekview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.*

/**a broadcast receiver that tells you when the time has changed (minute, date, configuration of time/date...), based on: https://stackoverflow.com/a/48782963/878126 */
abstract class TimeChangedBroadcastReceiver : BroadcastReceiver() {
    private var curCal = Calendar.getInstance()

    abstract fun onTimeChanged()

    @Suppress("MemberVisibilityCanBePrivate")
    fun register(context: Context, cal: Calendar) {
        curCal = cal
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_DATE_CHANGED)
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(this, filter)
        val newDate = Calendar.getInstance()
        if (!WeekViewUtil.isSameDayAndHourAndMinute(newDate, curCal)) {
            curCal = newDate
            onTimeChanged()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val newTime = Calendar.getInstance()
        if (!WeekViewUtil.isSameDayAndHourAndMinute(newTime, curCal)) {
            curCal = newTime
            onTimeChanged()
        }
    }
}
