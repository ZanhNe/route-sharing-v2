package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PreviewConsistencyToken(
        Long routeId,
        Long schoolId,
        Long routeVersion,
        Long actorUserId,
        Long actorUserVersion,
        Long actorSecurityVersion,
        Long driverId,
        Long driverUserVersion,
        Long driverSecurityVersion,
        Long driverProfileId,
        Long driverProfileVersion,
        Long vehicleId,
        Long vehicleVersion,
        Long modelId,
        Long modelVersion,
        Long brandId,
        Long brandVersion,
        Long actorMembershipId,
        Long actorMembershipVersion,
        Long driverMembershipId,
        Long driverMembershipVersion,
        Long schoolVersion,
        Long businessConfigId,
        Long businessConfigVersion,
        BigDecimal sameDestinationRadiusMeters,
        BigDecimal destinationNearRouteRadiusMeters,
        BigDecimal maxPickupDeviationMeters,
        Long maxPickupDeviationSeconds,
        BigDecimal minimumConvenienceRatioPercent,
        Long requestTtlSeconds,
        Long bookingCutoffSeconds,
        Long rejectionCooldownSeconds,
        Instant expectedDepartureTime,
        Integer remainingSeats) {

    public PreviewConsistencyToken {
        Objects.requireNonNull(routeId, "routeId không được trống");
        Objects.requireNonNull(schoolId, "schoolId không được trống");
        Objects.requireNonNull(routeVersion, "routeVersion không được trống");
        Objects.requireNonNull(actorUserId, "actorUserId không được trống");
        Objects.requireNonNull(actorUserVersion, "actorUserVersion không được trống");
        Objects.requireNonNull(actorSecurityVersion, "actorSecurityVersion không được trống");
        Objects.requireNonNull(driverId, "driverId không được trống");
        Objects.requireNonNull(driverUserVersion, "driverUserVersion không được trống");
        Objects.requireNonNull(driverSecurityVersion, "driverSecurityVersion không được trống");
        Objects.requireNonNull(driverProfileId, "driverProfileId không được trống");
        Objects.requireNonNull(driverProfileVersion, "driverProfileVersion không được trống");
        Objects.requireNonNull(vehicleId, "vehicleId không được trống");
        Objects.requireNonNull(vehicleVersion, "vehicleVersion không được trống");
        Objects.requireNonNull(modelId, "modelId không được trống");
        Objects.requireNonNull(modelVersion, "modelVersion không được trống");
        Objects.requireNonNull(brandId, "brandId không được trống");
        Objects.requireNonNull(brandVersion, "brandVersion không được trống");
        Objects.requireNonNull(actorMembershipId, "actorMembershipId không được trống");
        Objects.requireNonNull(actorMembershipVersion, "actorMembershipVersion không được trống");
        Objects.requireNonNull(driverMembershipId, "driverMembershipId không được trống");
        Objects.requireNonNull(driverMembershipVersion, "driverMembershipVersion không được trống");
        Objects.requireNonNull(schoolVersion, "schoolVersion không được trống");
        Objects.requireNonNull(businessConfigId, "businessConfigId không được trống");
        Objects.requireNonNull(businessConfigVersion, "businessConfigVersion không được trống");
        Objects.requireNonNull(sameDestinationRadiusMeters, "sameDestinationRadiusMeters không được trống");
        Objects.requireNonNull(destinationNearRouteRadiusMeters, "destinationNearRouteRadiusMeters không được trống");
        Objects.requireNonNull(maxPickupDeviationMeters, "maxPickupDeviationMeters không được trống");
        Objects.requireNonNull(maxPickupDeviationSeconds, "maxPickupDeviationSeconds không được trống");
        Objects.requireNonNull(minimumConvenienceRatioPercent,
                "minimumConvenienceRatioPercent không được trống");
        Objects.requireNonNull(requestTtlSeconds, "requestTtlSeconds không được trống");
        Objects.requireNonNull(bookingCutoffSeconds, "bookingCutoffSeconds không được trống");
        Objects.requireNonNull(rejectionCooldownSeconds, "rejectionCooldownSeconds không được trống");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống");
        Objects.requireNonNull(remainingSeats, "remainingSeats không được trống");
    }
}
