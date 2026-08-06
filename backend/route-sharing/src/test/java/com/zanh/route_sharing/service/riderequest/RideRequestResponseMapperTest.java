package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RideRequestResponseMapperTest {

    private final RideRequestResponseMapper sut = new RideRequestResponseMapper();

    @Test
    void givenPersistedCreationView_whenMapping_thenPublicResponsePreservesSnapshotAndNeverReservesSeat() {
        RideRequestResponse result = sut.toResponse(RideRequestMother.persistedView());

        assertThat(result.rideRequestId()).isEqualTo(501L);
        assertThat(result.routeId()).isEqualTo(RideRequestMother.ROUTE_ID);
        assertThat(result.status().name()).isEqualTo("PENDING");
        assertThat(result.seatReserved()).isFalse();
        assertThat(result.pickup().latitude()).isEqualByComparingTo("10.776530");
        assertThat(result.pickup().longitude()).isEqualByComparingTo("106.700981");
        assertThat(result.passengerDestination().address()).isEqualTo("Điểm đến");
        assertThat(result.proposedDropoff().address()).isEqualTo("Điểm thả");
        assertThat(result.passengerDesiredDistanceMeters()).isEqualByComparingTo("4200.00");
        assertThat(result.servedDistanceMeters()).isEqualByComparingTo("3900.00");
        assertThat(result.remainingDistanceMeters()).isEqualByComparingTo("300.00");
        assertThat(result.proposedSupportAmount()).isEqualByComparingTo("25000.00");
        assertThat(result.agreedSupportAmount()).isNull();
    }
}
