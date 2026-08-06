package com.zanh.route_sharing.repository.sharedroute.riderequest.decision.model;

public record CurrentAcceptEligibility(
        boolean driverAccountActive,
        boolean driverProfileActive,
        boolean membershipApproved,
        boolean vehicleActive,
        boolean vehicleUseRightValid,
        boolean vehicleModelActive,
        boolean vehicleBrandActive,
        boolean schoolActive) {

    public boolean eligible() {
        return driverAccountActive
                && driverProfileActive
                && membershipApproved
                && vehicleActive
                && vehicleUseRightValid
                && vehicleModelActive
                && vehicleBrandActive
                && schoolActive;
    }
}
