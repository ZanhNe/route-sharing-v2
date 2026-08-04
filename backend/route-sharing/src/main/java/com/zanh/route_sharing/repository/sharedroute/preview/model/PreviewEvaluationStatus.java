package com.zanh.route_sharing.repository.sharedroute.preview.model;

public enum PreviewEvaluationStatus {
    ELIGIBLE,
    NOT_FOUND_OR_INACCESSIBLE,
    ROUTE_UNAVAILABLE,
    SELF_ROUTE,
    DRIVER_OR_VEHICLE_INELIGIBLE,
    NO_LONGER_MATCHES
}
