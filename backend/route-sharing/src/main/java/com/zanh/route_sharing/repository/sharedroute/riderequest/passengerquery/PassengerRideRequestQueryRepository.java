package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery;

import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestQueryCriteria;

import java.util.Optional;

public interface PassengerRideRequestQueryRepository {

    PassengerRideRequestPageSnapshot findPage(PassengerRideRequestQueryCriteria criteria);

    Optional<PassengerRideRequestDetailRow> findDetail(Long actorUserId, Long rideRequestId);
}
