package com.zanh.route_sharing.service.tripcancellation;

import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartResponse;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripCancellationResponseMapper {

    public CancelTripBeforeStartResponse toResponse(TripCancellationCommitResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Kết quả hủy chuyến không được trống.");
        }
        return new CancelTripBeforeStartResponse(
                result.tripId(),
                result.routeId(),
                result.tripStatus(),
                result.routeStatus(),
                result.cancelledAt(),
                result.reason(),
                result.affectedBookingCount());
    }
}
