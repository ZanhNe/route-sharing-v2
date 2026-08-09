package com.zanh.route_sharing.service.routing.model;

/**
 * Validation profile for an ordered route-plan request.
 * STRICT preserves the accepted E1/E2 unique-role contract.
 * MULTI_PASSENGER is an explicit opt-in used by E4 trip formation.
 */
public enum RoutePlanRequestMode {
    STRICT,
    MULTI_PASSENGER
}
