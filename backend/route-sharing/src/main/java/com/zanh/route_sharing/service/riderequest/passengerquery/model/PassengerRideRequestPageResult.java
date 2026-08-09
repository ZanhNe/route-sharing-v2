package com.zanh.route_sharing.service.riderequest.passengerquery.model;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestPageResponse;

import java.util.Objects;

public record PassengerRideRequestPageResult(
        PassengerRideRequestPageResponse data,
        PageMeta meta) {

    public PassengerRideRequestPageResult {
        Objects.requireNonNull(data, "data không được trống.");
        Objects.requireNonNull(meta, "meta không được trống.");
    }
}
