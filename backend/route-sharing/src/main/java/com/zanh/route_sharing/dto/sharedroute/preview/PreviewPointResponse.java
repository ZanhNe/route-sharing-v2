package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;

public record PreviewPointResponse(
                BigDecimal latitude,
                BigDecimal longitude,
                String address) {
}
