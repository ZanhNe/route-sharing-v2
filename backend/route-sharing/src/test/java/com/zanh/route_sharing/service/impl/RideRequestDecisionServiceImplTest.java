package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.RideRequestDecisionRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.model.CurrentAcceptEligibility;
import com.zanh.route_sharing.service.riderequest.decision.RideRequestActionabilityPolicy;
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideRequestDecisionServiceImplTest {

        @Mock
        private RideRequestDecisionRepository repository;

        private RideRequestDecisionServiceImpl sut;

        @BeforeEach
        void setUp() {
                sut = new RideRequestDecisionServiceImpl(
                                repository,
                                new RideRequestActionabilityPolicy(),
                                Clock.fixed(RideRequestDecisionMother.DECISION_AT, ZoneOffset.UTC));
        }

        @Test
        void givenOwnedActionablePendingRequest_whenAccepting_thenSeatStateAuditAndNotificationChangeAtomically() {
                var aggregate = RideRequestDecisionMother.aggregate();
                stubCommon(aggregate);
                when(repository.evaluateCurrentAcceptEligibility(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                aggregate.configuration().getNhaTruong().getId(),
                                LocalDate.ofInstant(
                                                aggregate.route().getThoiGianKhoiHanhDuKien(),
                                                ZoneId.of("Asia/Ho_Chi_Minh"))))
                                .thenReturn(eligible());

                var result = sut.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID);

                assertThat(result.status()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
                assertThat(result.remainingSeats()).isEqualTo(1);
                assertThat(result.agreedSupportAmount())
                                .isEqualByComparingTo(RideRequestDecisionMother.PROPOSED_SUPPORT);
                assertThat(aggregate.route().getSoGheConLai()).isEqualTo(1);
                assertThat(aggregate.request().getChapNhanLuc()).isEqualTo(RideRequestDecisionMother.DECISION_AT);

                ArgumentCaptor<NhatKyTrangThaiYeuCau> event = ArgumentCaptor.forClass(NhatKyTrangThaiYeuCau.class);
                ArgumentCaptor<ThongBao> notification = ArgumentCaptor.forClass(ThongBao.class);
                verify(repository).appendStateLog(event.capture());
                verify(repository).persistNotification(notification.capture());
                verify(repository).flush();
                assertThat(event.getValue().getTrangThaiSau()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
                assertThat(notification.getValue().getNguoiNhan()).isSameAs(aggregate.passenger());
        }

        @Test
        void givenOwnedActionablePendingRequest_whenRejecting_thenSeatIsUnchangedAndCooldownIsSnapshotted() {
                var aggregate = RideRequestDecisionMother.aggregate();
                stubCommon(aggregate);

                var result = sut.reject(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID);

                assertThat(result.status()).isEqualTo(TrangThaiYeuCau.REJECTED);
                assertThat(result.remainingSeats()).isEqualTo(2);
                assertThat(result.cooldownUntil())
                                .isEqualTo(RideRequestDecisionMother.DECISION_AT.plusSeconds(3600));
                assertThat(aggregate.route().getSoGheConLai()).isEqualTo(2);
                verify(repository).appendStateLog(org.mockito.ArgumentMatchers.any(NhatKyTrangThaiYeuCau.class));
                verify(repository).persistNotification(org.mockito.ArgumentMatchers.any(ThongBao.class));
                verify(repository).flush();
                verify(repository, never()).evaluateCurrentAcceptEligibility(
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenMissingOrNonOwnedRoute_whenDeciding_thenNotFoundBeforeRequestLock() {
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.empty());

                assertBusinessCode(() -> sut.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "SHARED_ROUTE_NOT_FOUND");

                verify(repository, never()).lockRideRequest(
                                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        void givenMissingRequest_whenDeciding_thenRequestNotFoundWithoutMutation() {
                var aggregate = RideRequestDecisionMother.aggregate();
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.empty());

                assertBusinessCode(() -> sut.reject(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "RIDE_REQUEST_NOT_FOUND");
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
                verify(repository, never()).persistNotification(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenRequestAlreadyAccepted_whenCallingAgain_thenConflictAndSeatIsNotConsumedAgain() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.request().accept(RideRequestDecisionMother.DECISION_AT.minusSeconds(1));
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.of(aggregate.request()));

                assertBusinessCode(() -> sut.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "INVALID_RIDE_REQUEST_STATE");

                assertThat(aggregate.route().getSoGheConLai()).isEqualTo(2);
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenExpiredPendingRequest_whenDeciding_thenExpiredConflictWithoutSideEffects() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.request().setVersion(0L);
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.of(aggregate.request()));

                assertBusinessCode(() -> new RideRequestDecisionServiceImpl(
                                repository,
                                new RideRequestActionabilityPolicy(),
                                Clock.fixed(aggregate.request().getExpiresAt(), ZoneOffset.UTC))
                                .reject(RideRequestDecisionMother.ACTOR_ID,
                                                RideRequestDecisionMother.ROUTE_ID,
                                                RideRequestDecisionMother.REQUEST_ID),
                                "RIDE_REQUEST_EXPIRED");
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenRouteNotOpen_whenDeciding_thenRouteStateConflict() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.route().setTrangThaiLoTrinh(TrangThaiLoTrinh.CANCELLED);
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.of(aggregate.request()));

                assertBusinessCode(() -> sut.reject(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "SHARED_ROUTE_NOT_OPEN");
        }

        @Test
        void givenNoSeat_whenAccepting_thenNoSeatConflictAndRequestRemainsPending() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.route().setSoGheConLai(0);
                stubCommon(aggregate);
                when(repository.evaluateCurrentAcceptEligibility(
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.any())).thenReturn(eligible());

                assertBusinessCode(() -> sut.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "SHARED_ROUTE_NO_REMAINING_SEATS");
                assertThat(aggregate.request().getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.PENDING);
        }

        @Test
        void givenCurrentDriverOrVehicleIneligible_whenAccepting_thenEligibilityConflict() {
                var aggregate = RideRequestDecisionMother.aggregate();
                stubCommon(aggregate);
                when(repository.evaluateCurrentAcceptEligibility(
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new CurrentAcceptEligibility(true, false, true, true, true, true, true,
                                                true));

                assertBusinessCode(() -> sut.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "DRIVER_OR_VEHICLE_INELIGIBLE");
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenAcceptCutoffReached_whenAccepting_thenRequestAndSeatRemainUnchanged() {
                var aggregate = RideRequestDecisionMother.aggregate();
                stubCommon(aggregate);
                when(repository.evaluateCurrentAcceptEligibility(
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.anyLong(),
                                org.mockito.ArgumentMatchers.any())).thenReturn(eligible());

                aggregate.configuration().setBookingCutoffSeconds(7200L);
                RideRequestDecisionServiceImpl atCutoff = new RideRequestDecisionServiceImpl(
                                repository,
                                new RideRequestActionabilityPolicy(),
                                Clock.fixed(RideRequestDecisionMother.DECISION_AT, ZoneOffset.UTC));

                assertBusinessCode(() -> atCutoff.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "SHARED_ROUTE_BOOKING_CUTOFF_REACHED");
                assertThat(aggregate.route().getSoGheConLai()).isEqualTo(2);
                assertThat(aggregate.request().getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.PENDING);
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenCurrentConfigurationMissing_whenRejecting_thenConfigurationConflictHasNoSideEffect() {
                var aggregate = RideRequestDecisionMother.aggregate();
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.of(aggregate.request()));
                when(repository.lockCurrentConfiguration(aggregate.request())).thenReturn(Optional.empty());

                assertBusinessCode(() -> sut.reject(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID), "BUSINESS_CONFIGURATION_UNAVAILABLE");
                assertThat(aggregate.request().getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.PENDING);
                verify(repository, never()).appendStateLog(org.mockito.ArgumentMatchers.any());
                verify(repository, never()).persistNotification(org.mockito.ArgumentMatchers.any());
        }

        @Test
        void givenInvalidIds_whenDeciding_thenInputRejectedBeforeRepository() {
                assertBusinessCode(() -> sut.accept(9L, 0L, 1L), "INVALID_RIDE_REQUEST_DECISION_PATH");
                verifyNoMoreInteractions(repository);
        }

        private void stubCommon(RideRequestDecisionMother.DecisionAggregate aggregate) {
                when(repository.lockOwnedRoute(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID)).thenReturn(Optional.of(aggregate.route()));
                when(repository.lockRideRequest(
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(Optional.of(aggregate.request()));
                when(repository.lockCurrentConfiguration(aggregate.request()))
                                .thenReturn(Optional.of(aggregate.configuration()));
        }

        private static CurrentAcceptEligibility eligible() {
                return new CurrentAcceptEligibility(true, true, true, true, true, true, true, true);
        }

        private static void assertBusinessCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
                        String code) {
                assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                                exception -> assertThat(exception.getCode()).isEqualTo(code));
        }
}
