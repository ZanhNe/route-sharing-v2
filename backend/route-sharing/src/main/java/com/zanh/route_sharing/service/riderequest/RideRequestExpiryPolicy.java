package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class RideRequestExpiryPolicy {

    public Instant calculate(
            Instant sentAt,
            Instant expectedDepartureTime,
            Duration requestTtl,
            Duration bookingCutoff) {
        Objects.requireNonNull(sentAt, "sentAt không được trống");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống");
        Objects.requireNonNull(requestTtl, "requestTtl không được trống");
        Objects.requireNonNull(bookingCutoff, "bookingCutoff không được trống");
        if (requestTtl.isZero() || requestTtl.isNegative() || bookingCutoff.isNegative()) {
            throw new IllegalArgumentException("TTL/cutoff không hợp lệ");
        }

        final Instant ttlBoundary;
        final Instant cutoffBoundary;
        try {
            ttlBoundary = sentAt.plus(requestTtl);
            cutoffBoundary = expectedDepartureTime.minus(bookingCutoff);
        } catch (DateTimeException | ArithmeticException exception) {
            throw unavailable();
        }
        Instant expiresAt = ttlBoundary.isBefore(cutoffBoundary)
                ? ttlBoundary
                : cutoffBoundary;
        if (!expiresAt.isAfter(sentAt)) {
            throw unavailable();
        }
        return expiresAt;
    }

    private static BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "SHARED_ROUTE_BOOKING_CUTOFF_REACHED",
                "Lộ trình đã hết thời gian nhận yêu cầu đi chung.");
    }
}
