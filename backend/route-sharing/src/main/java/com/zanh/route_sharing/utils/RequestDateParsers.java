package com.zanh.route_sharing.utils;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class RequestDateParsers {
    public static final ZoneId VIETNAM_ZONE = TimePolicy.BUSINESS_ZONE;

    private RequestDateParsers() {
    }

    public static Instant parseStartInclusive(String value, String parameterName) {
        return parse(value, parameterName, false, VIETNAM_ZONE);
    }

    public static Instant parseEndExclusive(String value, String parameterName) {
        return parse(value, parameterName, true, VIETNAM_ZONE);
    }

    static Instant parse(String value, String parameterName, boolean endExclusive, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            if (normalized.endsWith("Z")) {
                return Instant.parse(normalized);
            }
            if (hasExplicitOffset(normalized)) {
                return OffsetDateTime.parse(normalized).toInstant();
            }
            if (normalized.contains("T")) {
                return LocalDateTime.parse(normalized).atZone(zoneId).toInstant();
            }
            LocalDate date = LocalDate.parse(normalized);
            return (endExclusive ? date.plusDays(1) : date).atStartOfDay(zoneId).toInstant();
        } catch (DateTimeException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_TIME",
                    parameterName + " không đúng định dạng ISO-8601.");
        }
    }

    private static boolean hasExplicitOffset(String value) {
        int t = value.indexOf('T');
        if (t < 0)
            return false;
        return value.indexOf('+', t) >= 0 || value.indexOf('-', t + 1) >= 0;
    }
}
