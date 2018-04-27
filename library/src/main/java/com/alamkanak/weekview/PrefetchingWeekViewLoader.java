package com.alamkanak.weekview;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * <h1>PrefetchingWeekViewLoader</h1>
 * This class provides prefetching data loading behaviour.
 * By setting a specific period of N, data is retrieved for the current period,
 * the next N periods and the previous N periods.
 */
public class PrefetchingWeekViewLoader implements WeekViewLoader {
    private WeekViewLoader mWeekViewLoader;
    private int mPrefetchingPeriod;

    /**
     * Constructor to use the default prefetching period of 1
     * @param weekViewLoader An instance of the WeekViewLoader class
     */
    public PrefetchingWeekViewLoader(WeekViewLoader weekViewLoader){
        this.mWeekViewLoader = weekViewLoader;
        this.mPrefetchingPeriod = 1;
    }

    /**
     * Constructor to set a custom prefetching period
     * @param weekViewLoader An instance of the WeekViewLoader class
     * @param prefetchingPeriod The amount of periods to be fetched before and after the
     *                          current period. Must be 1 or greater.
     */
    public PrefetchingWeekViewLoader(WeekViewLoader weekViewLoader, int prefetchingPeriod) throws IllegalArgumentException {
        this.mWeekViewLoader = weekViewLoader;

        if(prefetchingPeriod < 1){
            throw new IllegalArgumentException("Must specify prefetching period of at least 1!");
        }

        this.mPrefetchingPeriod = prefetchingPeriod;
    }

    public int getPrefetchingPeriod() {
        return mPrefetchingPeriod;
    }

    @Override
    public List<? extends WeekViewEvent> onLoad(int periodIndex) {
        // fetch the current period
        ArrayList<WeekViewEvent> events = new ArrayList<>(mWeekViewLoader.onLoad(periodIndex));

        // fetch periods before/after
        for(int i = 1; i <= this.mPrefetchingPeriod; i++){
            events.addAll(mWeekViewLoader.onLoad(periodIndex - i));
            events.addAll(mWeekViewLoader.onLoad(periodIndex + i));
        }

        // return list of all events together
        return events;
    }

    @Override
    public double toWeekViewPeriodIndex(Calendar instance) {
        return mWeekViewLoader.toWeekViewPeriodIndex(instance);
    }
}
