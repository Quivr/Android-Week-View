package com.alamkanak.weekview

import androidx.annotation.IntRange
import java.util.*

/**
 * <h1>PrefetchingWeekViewLoader</h1>
 * This class provides prefetching data loading behaviour.
 * By setting a specific period of N, data is retrieved for the current period,
 * the next N periods and the previous N periods.
 */
/**
 * @param weekViewLoader An instance of the WeekViewLoader class
 * @param prefetchingPeriod The amount of periods to be fetched before and after the
 * current period. Must be 1 or greater.
 */
class PrefetchingWeekViewLoader(val weekViewLoader: WeekViewLoader, @IntRange(from = 1L) val prefetchingPeriod: Int = 1) : WeekViewLoader {

    init {
        if (prefetchingPeriod < 1)
            throw IllegalArgumentException("Must specify prefetching period of at least 1!")
    }

    override fun onLoad(periodIndex: Int): MutableList<WeekViewEvent>? {
        // fetch the current period
        var loadedEvents = weekViewLoader.onLoad(periodIndex)
        val events = ArrayList<WeekViewEvent>()
        if (loadedEvents != null)
            events.addAll(loadedEvents)
        // fetch periods before/after
        for (i in 1..this.prefetchingPeriod) {
            loadedEvents = weekViewLoader.onLoad(periodIndex - i)
            if (loadedEvents != null)
                events.addAll(loadedEvents)
            loadedEvents = weekViewLoader.onLoad(periodIndex + i)
            if (loadedEvents != null)
                events.addAll(loadedEvents)
        }
        // return list of all events together
        return events
    }

    override fun toWeekViewPeriodIndex(instance: Calendar) = weekViewLoader.toWeekViewPeriodIndex(instance)

}
