package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.util.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public record TimestampRange(LocalDateTime start, LocalDateTime end) {
    public static TimestampRange lastNDays(int days) {
        LocalDateTime now = DateUtil.nowAsUTC();
        return new TimestampRange(now.minusDays(days), now);
    }

    public static TimestampRange thisMonth() {
        LocalDateTime localStartOfMonth = LocalDate.now(ZoneId.systemDefault()).atStartOfDay().withDayOfMonth(1);
        LocalDateTime utcStart = localStartOfMonth.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        return new TimestampRange(utcStart, DateUtil.nowAsUTC());
    }

    public static TimestampRange thisYear() {
        LocalDateTime utcStart = LocalDate.now(ZoneId.systemDefault())
                .withDayOfYear(1)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        return new TimestampRange(utcStart, DateUtil.nowAsUTC());
    }

    public static TimestampRange unbounded() {
        LocalDateTime now = DateUtil.nowAsUTC();
        return new TimestampRange(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC), now);
    }
}
