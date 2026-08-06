package com.zanh.route_sharing.service.riderequest.decision;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;

@Component
public class RideRequestActionabilityPolicy {

    public void requireActionable(Instant decisionAt, Instant expiresAt) {
        if (decisionAt == null || expiresAt == null || !decisionAt.isBefore(expiresAt)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RIDE_REQUEST_EXPIRED",
                    "Yêu cầu đi chung đã hết thời hạn xử lý.");
        }
    }

    public void requireAcceptWindowOpen(
            Instant decisionAt,
            Instant departureTime,
            long bookingCutoffSeconds) {
        if (decisionAt == null || departureTime == null || bookingCutoffSeconds < 0) {
            throw cutoffReached();
        }
        final Instant cutoffBoundary;
        try {
            cutoffBoundary = departureTime.minusSeconds(bookingCutoffSeconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw cutoffReached();
        }
        if (!decisionAt.isBefore(cutoffBoundary)) {
            throw cutoffReached();
        }
    }

    private static BusinessException cutoffReached() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "SHARED_ROUTE_BOOKING_CUTOFF_REACHED",
                "Lộ trình đã qua thời điểm tiếp nhận quyết định đi chung.");
    }
}
