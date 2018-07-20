package com.alamkanak.weekview;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Created by jesse on 6/02/2016.
 */
public class WeekViewUtil {


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
    public static boolean isSameDay(LocalDateTime dateOne, LocalDateTime dateTwo) {
        return dateOne.toLocalDate().isEqual(dateTwo.toLocalDate());
    }

    /**
     * Returns the amount of days between the second date and the first date
     *
     * @param dateOne the first date
     * @param dateTwo the second date
     * @return the amount of days between dateTwo and dateOne
     */
    public static long daysBetween(LocalDateTime dateOne, LocalDateTime dateTwo) {
        return Duration.between(dateOne, dateTwo).toDays();
    }

    /*
     * Returns the amount of minutes passed in the day before the time in the given date
     * @param date
     * @return amount of minutes in day before time
     */
    public static int getPassedMinutesInDay(LocalDateTime date) {
        return getPassedMinutesInDay(date.getHour(), date.getMinute());
    }

    /**
     * Returns the amount of minutes in the given hours and minutes
     *
     * @param hour Number of hours
     * @param minute Number of minutes
     * @return amount of minutes in the given hours and minutes
     */
    public static int getPassedMinutesInDay(int hour, int minute) {
        return hour * 60 + minute;
    }
}
