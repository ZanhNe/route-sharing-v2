package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.util.Objects;

public record SharedRoutePreviewPreparation(
        PreviewRouteSnapshot route,
        PreviewDriverSnapshot driver,
        PreviewVehicleSnapshot vehicle,
        PreviewMatch match,
        PreviewConsistencyToken consistencyToken) {

    public SharedRoutePreviewPreparation {
        Objects.requireNonNull(route, "route không được trống");
        Objects.requireNonNull(driver, "driver không được trống");
        Objects.requireNonNull(vehicle, "vehicle không được trống");
        Objects.requireNonNull(match, "match không được trống");
        Objects.requireNonNull(consistencyToken, "consistencyToken không được trống");
    }
}
