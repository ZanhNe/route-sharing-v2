package com.zanh.route_sharing.dto.sharedroute;

import java.math.BigDecimal;

public record RouteEndpointResponse(
                BigDecimal latitude,
                BigDecimal longitude,
                String address) {
}
