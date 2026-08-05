package com.zanh.route_sharing.service.riderequest.model;

import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;

import java.util.Objects;

public record RideRequestCreationResult(
        RideRequestResponse response,
        boolean replayed) {

    public RideRequestCreationResult {
        Objects.requireNonNull(response, "response không được trống");
    }
}
