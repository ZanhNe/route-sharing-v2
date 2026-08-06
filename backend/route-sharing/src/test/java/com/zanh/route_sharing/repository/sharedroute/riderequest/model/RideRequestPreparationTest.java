package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestPreparationTest {

    @Test
    void motherPreparation_isValidAndContainsPositiveSeatAvailability() {
        RideRequestPreparation result = RideRequestMother.segmentPreparation();

        assertThat(result.remainingSeats()).isEqualTo(2);
        assertThat(result.routeVersion()).isZero();
        assertThat(result.suggestedSupportPerKm()).isEqualByComparingTo("5000.00");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPreparations")
    void givenInvalidPreparationField_whenCreating_thenInvariantIsRejected(
            String description,
            UnaryOperator<PreparationValues> mutation) {
        PreparationValues values = mutation.apply(PreparationValues.valid());

        assertThatThrownBy(values::create)
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }

    private static Stream<Arguments> invalidPreparations() {
        return Stream.of(
                Arguments.of("route id", mutation(values -> values.routeId = 0L)),
                Arguments.of("route version", mutation(values -> values.routeVersion = -1L)),
                Arguments.of("driver", mutation(values -> values.driverId = 0L)),
                Arguments.of("vehicle", mutation(values -> values.vehicleType = null)),
                Arguments.of("departure", mutation(values -> values.departure = null)),
                Arguments.of("remaining seats", mutation(values -> values.remainingSeats = 0)),
                Arguments.of("negative suggested support",
                        mutation(values -> values.suggestedSupport = new BigDecimal("-0.01"))),
                Arguments.of("match type", mutation(values -> values.matchType = null)),
                Arguments.of("dropoff type", mutation(values -> values.dropoffType = null)),
                Arguments.of("pickup projection", mutation(values -> values.pickupProjection = null)),
                Arguments.of("proposed dropoff", mutation(values -> values.proposedDropoff = null)),
                Arguments.of("policy", mutation(values -> values.policy = null)),
                Arguments.of("token", mutation(values -> values.token = null)));
    }

    private static UnaryOperator<PreparationValues> mutation(
            java.util.function.Consumer<PreparationValues> consumer) {
        return values -> {
            consumer.accept(values);
            return values;
        };
    }

    private static final class PreparationValues {
        private Long routeId;
        private Long routeVersion;
        private Long driverId;
        private com.zanh.route_sharing.domain.enums.LoaiPhuongTien vehicleType;
        private java.time.Instant departure;
        private Integer remainingSeats;
        private BigDecimal suggestedSupport;
        private com.zanh.route_sharing.domain.enums.LoaiGhepTuyen matchType;
        private com.zanh.route_sharing.domain.enums.LoaiDiemTha dropoffType;
        private RideRequestGeoPoint pickupProjection;
        private RideRequestGeoPoint proposedDropoff;
        private com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot policy;
        private com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken token;

        static PreparationValues valid() {
            RideRequestPreparation source = RideRequestMother.segmentPreparation();
            PreparationValues values = new PreparationValues();
            values.routeId = source.routeId();
            values.routeVersion = source.routeVersion();
            values.driverId = source.driverId();
            values.vehicleType = source.vehicleType();
            values.departure = source.expectedDepartureTime();
            values.remainingSeats = source.remainingSeats();
            values.suggestedSupport = source.suggestedSupportPerKm();
            values.matchType = source.matchType();
            values.dropoffType = source.dropoffType();
            values.pickupProjection = source.pickupProjection();
            values.proposedDropoff = source.proposedDropoff();
            values.policy = source.policy();
            values.token = source.consistencyToken();
            return values;
        }

        RideRequestPreparation create() {
            return new RideRequestPreparation(
                    routeId,
                    routeVersion,
                    driverId,
                    vehicleType,
                    departure,
                    remainingSeats,
                    suggestedSupport,
                    matchType,
                    dropoffType,
                    pickupProjection,
                    proposedDropoff,
                    policy,
                    token);
        }
    }
}
