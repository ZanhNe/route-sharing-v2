package com.zanh.route_sharing.service.sharedroute.cancellation;

import com.zanh.route_sharing.dto.sharedroute.cancellation.CancelSharedRouteResponse;
import com.zanh.route_sharing.service.sharedroute.cancellation.model.SharedRouteCancellationResult;
import org.springframework.stereotype.Component;

@Component
public class SharedRouteCancellationResponseMapper {

    public CancelSharedRouteResponse toResponse(SharedRouteCancellationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Kết quả hủy lộ trình không được trống.");
        }
        return new CancelSharedRouteResponse(
                result.routeId(),
                result.previousStatus(),
                result.status(),
                result.cancelledAt(),
                result.reason(),
                result.pendingRequestsCancelled(),
                result.acceptedBookingsCancelled(),
                result.seatsRestored(),
                result.passengersNotified());
    }
}
