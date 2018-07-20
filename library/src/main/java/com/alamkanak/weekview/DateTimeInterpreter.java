package com.alamkanak.weekview;

import java.time.LocalDateTime;

/**
 * Created by Raquib on 1/6/2015.
 */
public interface DateTimeInterpreter {
    String interpretDate(LocalDateTime date);

    String interpretTime(int hour, int minutes);
}
