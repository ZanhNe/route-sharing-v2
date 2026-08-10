package com.zanh.route_sharing.domain.riderequest;

import java.time.DateTimeException;
import java.time.Instant;

public final class BookingWindowPolicy {

    private BookingWindowPolicy() {
    }

    public static Instant cutoffBoundary(Instant expectedDepartureTime, long bookingCutoffSeconds) {
        if (expectedDepartureTime == null || bookingCutoffSeconds < 0) {
            throw new IllegalArgumentException("Booking window input không hợp lệ.");
        }
        try {
            return expectedDepartureTime.minusSeconds(bookingCutoffSeconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new IllegalArgumentException("Không thể xác định booking cutoff.", exception);
        }
    }

    public static boolean isOpen(
            Instant actionAt,
            Instant expectedDepartureTime,
            long bookingCutoffSeconds) {
        if (actionAt == null) {
            throw new IllegalArgumentException("actionAt không được trống.");
        }
        return actionAt.isBefore(cutoffBoundary(expectedDepartureTime, bookingCutoffSeconds));
    }
}
