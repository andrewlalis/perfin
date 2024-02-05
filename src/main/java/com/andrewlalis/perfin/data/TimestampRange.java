package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.util.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record TimestampRange(LocalDateTime start, LocalDateTime end) {
    public static TimestampRange nDaysTillNow(int days) {
        LocalDateTime now = DateUtil.nowAsUTC();
        return new TimestampRange(now.minusDays(days), now);
    }

    public static TimestampRange unbounded() {
        LocalDateTime now = DateUtil.nowAsUTC();
        return new TimestampRange(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC), now);
    }
}
