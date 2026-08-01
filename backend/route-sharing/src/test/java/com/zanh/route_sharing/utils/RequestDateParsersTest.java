package com.zanh.route_sharing.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDateParsersTest {
    @Test
    void dateRangeUsesVietnamTimeAndExclusiveEnd() {
        assertThat(RequestDateParsers.parseStartInclusive("2026-08-01", "from"))
                .isEqualTo(Instant.parse("2026-07-31T17:00:00Z"));
        assertThat(RequestDateParsers.parseEndExclusive("2026-08-01", "to"))
                .isEqualTo(Instant.parse("2026-08-01T17:00:00Z"));
    }

    @Test
    void explicitOffsetIsPreserved() {
        assertThat(RequestDateParsers.parseStartInclusive("2026-08-01T07:30:00+07:00", "from"))
                .isEqualTo(Instant.parse("2026-08-01T00:30:00Z"));
    }
}
