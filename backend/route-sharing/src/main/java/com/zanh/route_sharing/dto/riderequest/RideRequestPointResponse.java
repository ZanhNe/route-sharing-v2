package com.zanh.route_sharing.dto.riderequest;

import java.math.BigDecimal;

public record RideRequestPointResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String address) {
}
