package com.zanh.route_sharing.service.sharedroute.driverquery.model;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRoutePageResponse;

import java.util.Objects;

public record DriverSharedRoutePageResult(
        DriverSharedRoutePageResponse data,
        PageMeta meta) {
    public DriverSharedRoutePageResult {
        Objects.requireNonNull(data, "data không được trống.");
        Objects.requireNonNull(meta, "meta không được trống.");
    }
}
