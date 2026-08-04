package com.zanh.route_sharing.dto.sharedroute.preview;

public record PreviewPointsResponse(
                PreviewPointResponse driverOrigin,
                PreviewPointResponse passengerPickup,
                PreviewPointResponse passengerDestination,
                PreviewPointResponse proposedDropoff,
                PreviewPointResponse driverDestination) {
}
