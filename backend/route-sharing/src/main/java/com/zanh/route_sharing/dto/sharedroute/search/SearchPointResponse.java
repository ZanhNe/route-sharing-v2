package com.zanh.route_sharing.dto.sharedroute.search;

import java.math.BigDecimal;

public record SearchPointResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String address
) {
}
