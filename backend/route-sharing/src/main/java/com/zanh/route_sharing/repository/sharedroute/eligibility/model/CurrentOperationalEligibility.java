package com.zanh.route_sharing.repository.sharedroute.eligibility.model;

public record CurrentOperationalEligibility(
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

    public static CurrentOperationalEligibility ineligible() {
        return new CurrentOperationalEligibility(false, false, false, false, false, false, false, false);
    }
}
