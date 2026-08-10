package com.zanh.route_sharing.service.riderequest.decision;

import com.zanh.route_sharing.domain.riderequest.BookingWindowPolicy;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RideRequestActionabilityPolicy {

    public void requireAcceptWindowOpen(
            Instant decisionAt,
            Instant expectedDepartureTime,
            Long bookingCutoffSeconds) {
        if (decisionAt == null || expectedDepartureTime == null
                || bookingCutoffSeconds == null || bookingCutoffSeconds < 0) {
            throw unavailableConfiguration();
        }
        final boolean open;
        try {
            open = BookingWindowPolicy.isOpen(decisionAt, expectedDepartureTime, bookingCutoffSeconds);
        } catch (IllegalArgumentException exception) {
            throw unavailableConfiguration();
        }
        if (!open) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_BOOKING_CUTOFF_REACHED",
                    "Lộ trình đã hết thời gian nhận yêu cầu đi chung.");
        }
    }

    private static BusinessException unavailableConfiguration() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFIGURATION_UNAVAILABLE",
                "Không thể xác định thời hạn nhận yêu cầu của lộ trình.");
    }
}
