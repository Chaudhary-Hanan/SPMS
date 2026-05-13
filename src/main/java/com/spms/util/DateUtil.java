package com.spms.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Utility methods for human-readable date/time formatting. */
public final class DateUtil {

    private static final DateTimeFormatter DATE_FULL   = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATE_SHORT  = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter DATE_DAY    = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter DATE_HEADER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter DT_FMT      = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");
    private static final DateTimeFormatter TIME_FMT    = DateTimeFormatter.ofPattern("hh:mm a");

    private DateUtil() {}

    public static String formatDate(LocalDate d) {
        return d == null ? "No date" : d.format(DATE_FULL);
    }

    public static String formatDateShort(LocalDate d) {
        return d == null ? "" : d.format(DATE_SHORT);
    }

    public static String formatDateDay(LocalDate d) {
        return d == null ? "" : d.format(DATE_DAY);
    }

    public static String formatHeaderDate() {
        return LocalDate.now().format(DATE_HEADER);
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "No date" : dt.format(DT_FMT);
    }

    public static String formatTime(LocalTime t) {
        return t == null ? "" : t.format(TIME_FMT);
    }

    /**
     * Returns a relative label:
     * "Overdue!", "Today", "Tomorrow", "In N days", "In N weeks", or date string.
     */
    public static String getRelativeDate(LocalDate d) {
        if (d == null) return "";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days <  0) return "Overdue!";
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        if (days <  7) return "In " + days + " days";
        if (days < 30) return "In " + (days / 7) + " week" + (days / 7 == 1 ? "" : "s");
        return formatDateShort(d);
    }

    /**
     * Live countdown string: "Xd Xh Xm" if days remain,
     * "HH:MM:SS" for same-day countdowns.
     */
    public static String formatCountdown(LocalDateTime target) {
        if (target == null) return "--";
        Duration d = Duration.between(LocalDateTime.now(), target);
        if (d.isNegative()) return "Passed";
        long days  = d.toDays();
        long hours = d.toHours()   % 24;
        long mins  = d.toMinutes() % 60;
        long secs  = d.getSeconds() % 60;
        if (days > 0)  return days + "d " + hours + "h " + mins + "m";
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    public static String formatDuration(int minutes) {
        if (minutes <= 0) return "0 min";
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }
}
