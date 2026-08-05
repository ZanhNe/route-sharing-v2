package com.zanh.route_sharing.service;

import com.zanh.route_sharing.service.riderequest.model.LocationLabel;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;

public interface LocationLabelResolver {

    LocationLabel resolve(GeoCoordinate coordinate);
}
