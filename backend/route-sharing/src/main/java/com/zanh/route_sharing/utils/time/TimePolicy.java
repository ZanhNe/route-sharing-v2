package com.zanh.route_sharing.utils.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class TimePolicy {

    public static final String BUSINESS_ZONE_ID = "Asia/Ho_Chi_Minh";
    public static final ZoneId BUSINESS_ZONE = ZoneId.of(BUSINESS_ZONE_ID);

    private TimePolicy() {
    }

    public static Instant now(Clock clock) {
        Objects.requireNonNull(clock, "clock không được trống");
        return databasePrecision(clock.instant());
    }

    public static Instant databasePrecision(Instant instant) {
        Objects.requireNonNull(instant, "instant không được trống");
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public static Instant tokenPrecision(Instant instant) {
        Objects.requireNonNull(instant, "instant không được trống");
        return instant.truncatedTo(ChronoUnit.MILLIS);
    }

    public static Instant tokenNow(Clock clock) {
        Objects.requireNonNull(clock, "clock không được trống");
        return tokenPrecision(clock.instant());
    }
}
