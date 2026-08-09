package com.zanh.route_sharing.service;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse;
import com.zanh.route_sharing.service.riderequest.passengerquery.model.PassengerRideRequestPageResult;

public interface PassengerRideRequestQueryService {

        PassengerRideRequestPageResult listOwnRideRequests(
                        Long actorUserId,
                        TrangThaiYeuCau status,
                        int page,
                        int size);

        PassengerRideRequestDetailResponse getOwnRideRequestDetail(
                        Long actorUserId,
                        Long rideRequestId);
}
