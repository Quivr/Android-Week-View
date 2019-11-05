package com.alamkanak.weekview;

import java.util.Calendar;

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
    public static boolean isSameDay(Calendar dateOne, Calendar dateTwo) {
        return dateOne.get(Calendar.YEAR) == dateTwo.get(Calendar.YEAR) && dateOne.get(Calendar.DAY_OF_YEAR) == dateTwo.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns a calendar instance at the start of today
     *
     * @return the calendar instance
     */
    public static Calendar today() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    /**
     * Checks if two dates are on the same day and hour.
     *
     * @param dateOne The first day.
     * @param dateTwo The second day.
     * @return Whether the dates are on the same day and hour.
     */
    public static boolean isSameDayAndHour(Calendar dateOne, Calendar dateTwo) {

        if (dateTwo != null) {
            return isSameDay(dateOne, dateTwo) && dateOne.get(Calendar.HOUR_OF_DAY) == dateTwo.get(Calendar.HOUR_OF_DAY);
        }
        return false;
    }

    /**
     * Returns the amount of days between the second date and the first date
     *
     * @param dateOne the first date
     * @param dateTwo the second date
     * @return the amount of days between dateTwo and dateOne
     */
    public static int daysBetween(Calendar dateOne, Calendar dateTwo) {
        return (int) (((dateTwo.getTimeInMillis() + dateTwo.getTimeZone().getOffset(dateTwo.getTimeInMillis())) / (1000 * 60 * 60 * 24)) -
                ((dateOne.getTimeInMillis() + dateOne.getTimeZone().getOffset(dateOne.getTimeInMillis())) / (1000 * 60 * 60 * 24)));
    }

    /*
    * Returns the amount of minutes passed in the day before the time in the given date
    * @param date
    * @return amount of minutes in day before time
    */
    public static int getPassedMinutesInDay(Calendar date) {
        return getPassedMinutesInDay(date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE));
    }

    /**
     * Returns the amount of minutes in the given hours and minutes
     *
     * @param hour
     * @param minute
     * @return amount of minutes in the given hours and minutes
     */
    public static int getPassedMinutesInDay(int hour, int minute) {
        return hour * 60 + minute;
    }
    
    /**
     * Checks if date2 is at the start of the next day after date1.
     * For example, if date1 was January the 1st and date2 was January the 2nd at 00:00,
     * this method would return true.
     * @param date1
     * @param date2
     * @return Whether or not date2 is at the start of the day after date1
     */
    public static boolean isAtStartOfNewDay(Calendar date1, Calendar date2){
        if(date2.get(Calendar.HOUR) == 0 &&
                date2.get(Calendar.MINUTE) == 0 &&
                date2.get(Calendar.SECOND) == 0 &&
                date2.get(Calendar.MILLISECOND) == 0){
            date2.add(Calendar.MILLISECOND, -1);
            if(isSameDay(date1, date2)){
                return true;
            }
        }
        return false;
    }
}
