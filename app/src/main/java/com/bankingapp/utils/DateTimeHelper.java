package com.bankingapp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * DateTimeHelper — central utility for all date/time operations.
 *
 * Syllabus: Date/Time API, SimpleDateFormat, Calendar
 *
 * All timestamps in the database are stored as:  "yyyy-MM-dd HH:mm:ss"
 * Display format shown to the user:               "dd MMM yyyy, hh:mm a"
 */
public class DateTimeHelper {

    // ── Format constants ───────────────────────────────────────────────
    /** DB storage format — used when inserting/querying transactions */
    public static final String DB_FORMAT      = "yyyy-MM-dd HH:mm:ss";

    /** User-facing display format — shown in the UI */
    public static final String DISPLAY_FORMAT = "dd MMM yyyy, hh:mm a";

    /** Month-only format — used by InsightsActivity Spinner filter */
    public static final String MONTH_FORMAT   = "yyyy-MM";

    /** Short date format — used in filter buttons */
    public static final String DATE_ONLY      = "yyyy-MM-dd";

    // ── Current date/time ─────────────────────────────────────────────

    /** Returns the current date+time formatted for DB storage. */
    public static String nowForDb() {
        return new SimpleDateFormat(DB_FORMAT, Locale.getDefault())
                .format(new Date());
    }

    /** Returns current date+time in a human-readable display format. */
    public static String nowForDisplay() {
        return new SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault())
                .format(new Date());
    }

    /** Returns the current month in "yyyy-MM" for Insights filter. */
    public static String currentMonth() {
        return new SimpleDateFormat(MONTH_FORMAT, Locale.getDefault())
                .format(new Date());
    }

    /** Returns today's date as "yyyy-MM-dd". */
    public static String today() {
        return new SimpleDateFormat(DATE_ONLY, Locale.getDefault())
                .format(new Date());
    }

    // ── Conversion ────────────────────────────────────────────────────

    /**
     * Convert a DB-format timestamp to a user-friendly display string.
     * e.g.  "2026-02-19 10:30:00"  →  "19 Feb 2026, 10:30 AM"
     */
    public static String toDisplayFormat(String dbTimestamp) {
        try {
            SimpleDateFormat dbFmt   = new SimpleDateFormat(DB_FORMAT,      Locale.getDefault());
            SimpleDateFormat dispFmt = new SimpleDateFormat(DISPLAY_FORMAT,  Locale.getDefault());
            Date parsed = dbFmt.parse(dbTimestamp);
            return parsed != null ? dispFmt.format(parsed) : dbTimestamp;
        } catch (ParseException e) {
            return dbTimestamp; // return raw string on parse failure
        }
    }

    /**
     * Returns a Calendar set to the start of today (00:00:00).
     * Useful for date-range filter comparisons.
     */
    public static Calendar startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE,      0);
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Returns a Calendar set to the end of today (23:59:59).
     */
    public static Calendar endOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE,      59);
        cal.set(Calendar.SECOND,      59);
        return cal;
    }

    /**
     * Returns a formatted string combining date and time for the
     * dashboard header: "Monday, 23 Feb 2026  |  10:30 AM"
     */
    public static String getDashboardHeader() {
        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a",            Locale.getDefault());
        Date now = new Date();
        return dayFmt.format(now) + "  |  " + timeFmt.format(now);
    }
}
