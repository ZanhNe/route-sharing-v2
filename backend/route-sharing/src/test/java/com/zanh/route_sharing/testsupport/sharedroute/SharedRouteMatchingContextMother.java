package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;

import java.math.BigDecimal;

public final class SharedRouteMatchingContextMother {

    private SharedRouteMatchingContextMother() {
    }

    public static SharedRouteMatchingContext standardConfiguration() {
        return new SharedRouteMatchingContext(
                new BigDecimal("500"),
                new BigDecimal("300"),
                new BigDecimal("200"),
                30);
    }
}
