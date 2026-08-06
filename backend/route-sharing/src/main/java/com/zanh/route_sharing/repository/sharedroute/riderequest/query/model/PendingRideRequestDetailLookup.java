package com.zanh.route_sharing.repository.sharedroute.riderequest.query.model;

import java.util.Objects;

public record PendingRideRequestDetailLookup(
        Status status,
        OwnedRouteSnapshot route,
        PendingRideRequestDetailRow request) {

    public PendingRideRequestDetailLookup {
        Objects.requireNonNull(status, "status không được trống.");
        if (status == Status.FOUND) {
            Objects.requireNonNull(route, "route không được trống khi trạng thái là FOUND.");
            Objects.requireNonNull(request, "request không được trống khi trạng thái là FOUND.");
        }
    }

    public static PendingRideRequestDetailLookup routeNotFound() {
        return new PendingRideRequestDetailLookup(Status.ROUTE_NOT_FOUND_OR_NOT_OWNED, null, null);
    }

    public static PendingRideRequestDetailLookup requestNotFound(OwnedRouteSnapshot route) {
        return new PendingRideRequestDetailLookup(
                Status.REQUEST_NOT_FOUND_OR_NOT_PENDING,
                Objects.requireNonNull(route),
                null);
    }

    public static PendingRideRequestDetailLookup found(
            OwnedRouteSnapshot route,
            PendingRideRequestDetailRow request) {
        return new PendingRideRequestDetailLookup(Status.FOUND, route, request);
    }

    public enum Status {
        FOUND,
        ROUTE_NOT_FOUND_OR_NOT_OWNED,
        REQUEST_NOT_FOUND_OR_NOT_PENDING
    }
}
