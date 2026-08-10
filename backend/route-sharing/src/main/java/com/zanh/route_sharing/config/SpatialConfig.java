package com.zanh.route_sharing.config;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpatialConfig {
    @Bean
    GeometryFactory wgs84GeometryFactory() {
        return new GeometryFactory(new PrecisionModel(), Wgs84Coordinates.SRID);
    }
}
