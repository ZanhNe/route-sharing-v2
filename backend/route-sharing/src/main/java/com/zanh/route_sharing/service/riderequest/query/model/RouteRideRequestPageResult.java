package com.zanh.route_sharing.service.riderequest.query.model;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestPageResponse;

import java.util.Objects;

public record RouteRideRequestPageResult(
        RouteRideRequestPageResponse data,
        PageMeta meta) {

    public RouteRideRequestPageResult {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(meta, "meta must not be null");
    }
}
