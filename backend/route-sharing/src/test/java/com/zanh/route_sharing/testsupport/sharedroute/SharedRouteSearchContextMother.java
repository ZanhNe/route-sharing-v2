package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.repository.SharedRouteSearchContext;

import java.math.BigDecimal;

public final class SharedRouteSearchContextMother {

    private SharedRouteSearchContextMother() {
    }

    public static SharedRouteSearchContext standardConfiguration() {
        return new SharedRouteSearchContext(
                new BigDecimal("500"),
                new BigDecimal("300"),
                new BigDecimal("200"),
                30);
    }
}
