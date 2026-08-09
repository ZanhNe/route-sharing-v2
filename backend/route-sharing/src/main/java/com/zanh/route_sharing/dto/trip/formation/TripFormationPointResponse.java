package com.zanh.route_sharing.dto.trip.formation;

import java.math.BigDecimal;

public record TripFormationPointResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String address) {
}
