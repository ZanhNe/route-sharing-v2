package com.zanh.route_sharing.service;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.integration.goong.RouteCalculation;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;

public interface GoongRouteService {

    RouteCalculation calculate(
            RouteCoordinate origin,
            RouteCoordinate destination,
            LoaiPhuongTien vehicleType);
}