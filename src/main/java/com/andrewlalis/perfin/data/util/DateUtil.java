package com.andrewlalis.perfin.data.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static DateTimeFormatter DEFAULT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter DEFAULT_DATETIME_FORMAT_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    public static DateTimeFormatter DEFAULT_DATETIME_FORMAT_PRECISE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatUTCAsLocalWithZone(LocalDateTime utcTimestamp) {
        return utcTimestamp.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DEFAULT_DATETIME_FORMAT_WITH_ZONE);
    }

    public static String formatUTCAsLocal(LocalDateTime utcTimestamp) {
        return utcTimestamp.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DEFAULT_DATETIME_FORMAT);
    }

    public static LocalDateTime localToUTC(LocalDateTime localTime, ZoneId localZone) {
        return localTime.atZone(localZone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime localToUTC(LocalDateTime localTime) {
        return localToUTC(localTime, ZoneId.systemDefault());
    }

    public static LocalDateTime nowAsUTC() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
