/**
 * HourOfDay
 * - Calculate hour-of-day from UTC timestamp
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.lang.Math;

class HourOfDay {

    // everything is in seconds !!

    static final int NUMBER_TIME_SPACES = 24;       // 24 hours
    static final int TIME_GAP_BETW_SPACES = Const.HOUR;   // 60min * 60sec
    static final int TIME_OFFSET = -7;              // Pacific time + day light saving

    public static LogEventInterface logInt = null;

    public static int get(long time) {
        int tod = (int)(((time / (long)TIME_GAP_BETW_SPACES) % (long)NUMBER_TIME_SPACES) + TIME_OFFSET);
        if (tod < 0) tod += 24;
        return tod;
    }

    public static int diff(long this_time, long curr_time) {
        int this_time_tod = get(this_time);
        int curr_time_tod = get(curr_time);
        int this_time_diff = Math.abs(this_time_tod - curr_time_tod);
        if (this_time_diff > 12) { this_time_diff = 24 - this_time_diff; }
        return this_time_diff;
    }

    public static int is_closer_time_of_day(long this_time, long than_time, long curr_time) {
        int this_time_diff = diff(this_time, curr_time);
        int than_time_diff = diff(than_time, curr_time);

        if (than_time <= 0) { return 1; }

        if (this_time_diff < than_time_diff) {
            println("   --> closer time of day (" + this_time_diff + " < " + than_time_diff + ")");
            return 1;
        } else if (this_time_diff > than_time_diff) {
            return -1;
        }
        return 0;
    }

    public static int is_same_time_of_day(long this_time, long curr_time) {
        int this_time_diff = diff(this_time, curr_time);
        
        if (this_time_diff <= 1) {
            println("   --> same time of day (this_time_diff = " + this_time_diff + ")");
            return 1;
        }
        return 0;
    }
    
    public static void println(String str) { if (logInt != null) logInt.println(str); }
}

