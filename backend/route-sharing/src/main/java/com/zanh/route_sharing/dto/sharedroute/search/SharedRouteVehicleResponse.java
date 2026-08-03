package com.zanh.route_sharing.dto.sharedroute.search;

public record SharedRouteVehicleResponse(
        Long id,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName
) {
}
