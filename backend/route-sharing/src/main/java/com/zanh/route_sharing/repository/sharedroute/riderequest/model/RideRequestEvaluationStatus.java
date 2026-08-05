package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

public enum RideRequestEvaluationStatus {
    ELIGIBLE,
    NOT_FOUND_OR_INACCESSIBLE,
    ROUTE_UNAVAILABLE,
    SELF_ROUTE,
    DRIVER_OR_VEHICLE_INELIGIBLE,
    NO_LONGER_MATCHES,
    UNFINISHED_REQUEST_EXISTS,
    REJECTION_COOLDOWN_ACTIVE
}
