package com.zanh.route_sharing.config;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.service.routing.RoutePlanningPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingPolicyConfig {

    @Bean
    RoutePlanningPolicy routePlanningPolicy(GoongProperties properties) {
        return new RoutePlanningPolicy(
                properties.getDuplicateWaypointToleranceMeters(),
                properties.getWaypointSnapToleranceMeters());
    }
}
