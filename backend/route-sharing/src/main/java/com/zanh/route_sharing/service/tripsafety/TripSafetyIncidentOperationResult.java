package com.zanh.route_sharing.service.tripsafety;

import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentResponse;

public record TripSafetyIncidentOperationResult(
        TripSafetyIncidentResponse response,
        boolean createdNew) {
    public TripSafetyIncidentOperationResult {
        if (response == null) {
            throw new IllegalArgumentException("response không được trống.");
        }
    }
}
