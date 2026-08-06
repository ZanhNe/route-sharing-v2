package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestEvaluationTest {

        @Test
        void eligibleFactory_containsPreparationAndCanRequireIt() {
                RideRequestPreparation preparation = RideRequestMother.segmentPreparation();
                RideRequestEvaluation result = RideRequestEvaluation.eligible(preparation);

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.ELIGIBLE);
                assertThat(result.requirePreparation()).isSameAs(preparation);
        }

        @ParameterizedTest
        @EnumSource(value = RideRequestEvaluationStatus.class, names = {
                        "NOT_FOUND_OR_INACCESSIBLE",
                        "ROUTE_UNAVAILABLE",
                        "SELF_ROUTE",
                        "DRIVER_OR_VEHICLE_INELIGIBLE",
                        "NO_LONGER_MATCHES"
        })
        void ineligibleFactory_containsNoPreparation(RideRequestEvaluationStatus status) {
                RideRequestEvaluation result = RideRequestEvaluation.ineligible(status);

                assertThat(result.status()).isEqualTo(status);
                assertThatThrownBy(result::requirePreparation)
                                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void unfinishedFactory_requiresExistingRequestIdAndStatus() {
                RideRequestEvaluation result = RideRequestEvaluation.unfinished(
                                99L,
                                TrangThaiYeuCau.ACCEPTED);

                assertThat(result.existingRideRequestId()).isEqualTo(99L);
                assertThat(result.existingStatus()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
                assertThatThrownBy(() -> RideRequestEvaluation.unfinished(null, TrangThaiYeuCau.PENDING))
                                .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> RideRequestEvaluation.unfinished(99L, null))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void cooldownFactory_requiresCooldownUntil() {
                Instant until = RideRequestMother.NOW.plusSeconds(3600);
                RideRequestEvaluation result = RideRequestEvaluation.cooldown(until);

                assertThat(result.cooldownUntil()).isEqualTo(until);
                assertThatThrownBy(() -> RideRequestEvaluation.cooldown(null))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EnumSource(value = RideRequestEvaluationStatus.class, names = {
                        "ELIGIBLE",
                        "UNFINISHED_REQUEST_EXISTS",
                        "REJECTION_COOLDOWN_ACTIVE"
        })
        void genericIneligibleFactory_rejectsSpecializedStatuses(RideRequestEvaluationStatus status) {
                assertThatThrownBy(() -> RideRequestEvaluation.ineligible(status))
                                .isInstanceOf(IllegalArgumentException.class);
        }
}
