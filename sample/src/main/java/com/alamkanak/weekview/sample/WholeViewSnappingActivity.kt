package com.alamkanak.weekview.sample

import android.graphics.RectF
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekDaySubtitleInterpreter
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
import kotlinx.android.synthetic.main.activity_base.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * Activity to demonstrate snapping of the whole view, for example week-by-week.
 */
class WholeViewSnappingActivity : BasicActivity() {
    val locale = Locale.getDefault()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        weekView.isShowNowLine = true
//        weekView.setAutoLimitTime(true)
        weekView.setLimitTime(0, 24)
        weekView.isUsingCheckersStyle = true
        weekView.columnGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
        weekView.hourSeparatorHeight = weekView.columnGap
        weekView.isScrollNumberOfVisibleDays = true
        weekView.dropListener = null
        weekView.allDaySideTitleText = getString(R.string.all_day)
        setDayViewType(TYPE_WEEK_VIEW)
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
        weekView.goToHour(Math.max(currentHour - 1, 0.0))
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        weekView.goToDate(cal)
        weekView.scrollListener = object : WeekView.ScrollListener {
            val monthFormatter = SimpleDateFormat("MMM", locale)
            val yearFormatter = SimpleDateFormat("yyyy", locale)

            override fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?) {
                //we show just the month here, so no need to update it every time
                if (oldFirstVisibleDay == null || oldFirstVisibleDay.get(Calendar.MONTH) != newFirstVisibleDay.get(Calendar.MONTH)) {
                    val date = newFirstVisibleDay.time
                    if (cal.get(Calendar.YEAR) == newFirstVisibleDay.get(Calendar.YEAR)) {
                        weekView.sideSubtitleText = ""
                        weekView.sideTitleText = monthFormatter.format(date)
                    } else {
                        weekView.sideTitleText = monthFormatter.format(date)
                        weekView.sideSubtitleText = yearFormatter.format(date)
                    }
                }
            }
        }
        draggable_view.visibility = View.GONE
        weekView.weekDaySubtitleInterpreter = object : WeekDaySubtitleInterpreter {
            val dateFormatTitle = SimpleDateFormat("d", locale)

            override fun getFormattedWeekDaySubtitle(date: Calendar): String = dateFormatTitle.format(date.time)
        }
        weekView.eventClickListener = object : WeekView.EventClickListener {
            override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
            }
        }
    }

    override fun setupDateTimeInterpreter(shortDate: Boolean) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timeFormat = DateFormat.getTimeFormat(this)
                ?: SimpleDateFormat("HH:mm", locale)
        val dateFormatTitle = SimpleDateFormat("EEE", locale)
        weekView.dateTimeInterpreter = object : DateTimeInterpreter {
            override fun getFormattedTimeOfDay(hour: Int, minutes: Int): String {
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minutes)
                return timeFormat.format(calendar.time)
            }

            override fun getFormattedWeekDayTitle(date: Calendar): String = dateFormatTitle.format(date.time)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_today) {
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
            weekView.goToDate(cal)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        menu.findItem(R.id.action_day_view).isChecked = false
        menu.findItem(R.id.action_three_day_view).isChecked = false
        menu.findItem(R.id.action_week_view).isChecked = true
        return result
    }
}
