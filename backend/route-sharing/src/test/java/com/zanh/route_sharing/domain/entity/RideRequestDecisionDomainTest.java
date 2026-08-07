package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiThongBao;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestDecisionDomainTest {

        @Test
        void givenPendingRequest_whenAccepting_thenStateTimeAndAgreedSupportAreChangedOnlyOnce() {
                var aggregate = RideRequestDecisionMother.aggregate();

                aggregate.request().accept(RideRequestDecisionMother.DECISION_AT);

                assertThat(aggregate.request().getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
                assertThat(aggregate.request().getChapNhanLuc()).isEqualTo(RideRequestDecisionMother.DECISION_AT);
                assertThat(aggregate.request().getMucHoTroDaThoaThuan())
                                .isEqualByComparingTo(RideRequestDecisionMother.PROPOSED_SUPPORT);
                assertThat(aggregate.request().getTuChoiLuc()).isNull();
                assertThatThrownBy(
                                () -> aggregate.request().accept(RideRequestDecisionMother.DECISION_AT.plusSeconds(1)))
                                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void givenPendingRequest_whenRejecting_thenCooldownAndConfigurationProvenanceAreSnapshotted() {
                var aggregate = RideRequestDecisionMother.aggregate();

                aggregate.request().reject(
                                RideRequestDecisionMother.DECISION_AT,
                                aggregate.configuration(),
                                3600L);

                assertThat(aggregate.request().getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.REJECTED);
                assertThat(aggregate.request().getTuChoiLuc()).isEqualTo(RideRequestDecisionMother.DECISION_AT);
                assertThat(aggregate.request().getCooldownUntil())
                                .isEqualTo(RideRequestDecisionMother.DECISION_AT.plusSeconds(3600));
                assertThat(aggregate.request().getRejectionCooldownAppliedSeconds()).isEqualTo(3600L);
                assertThat(aggregate.request().getCauHinhLucTuChoi()).isSameAs(aggregate.configuration());
                assertThat(aggregate.request().getCauHinhVersionLucTuChoi()).isEqualTo(3L);
                assertThat(aggregate.request().getMucHoTroDaThoaThuan()).isNull();
        }

        // @Tests
        // void
        // givenDecisionAtExpiryBoundary_whenAcceptingOrRejecting_thenTransitionIsRejected()
        // {
        // var acceptAggregate = RideRequestDecisionMother.aggregate();
        // var rejectAggregate = RideRequestDecisionMother.aggregate();
        // var boundary = acceptAggregate.request().getExpiresAt();

        // assertThatThrownBy(() -> acceptAggregate.request().accept(boundary))
        // .isInstanceOf(IllegalStateException.class);
        // assertThatThrownBy(() -> rejectAggregate.request().reject(
        // boundary,
        // rejectAggregate.configuration(),
        // 3600L)).isInstanceOf(IllegalStateException.class);
        // }

        @Test
        void givenOpenRouteWithSeats_whenAllocating_thenExactlyOneSeatIsConsumed() {
                var route = RideRequestDecisionMother.aggregate().route();

                route.allocateOneSeat();
                route.allocateOneSeat();

                assertThat(route.getSoGheConLai()).isZero();
                assertThatThrownBy(route::allocateOneSeat).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void givenRejectedRequest_whenCreatingAuditAndNotification_thenPassengerAndTransitionAreCorrect() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.request().reject(
                                RideRequestDecisionMother.DECISION_AT,
                                aggregate.configuration(),
                                3600L);

                var event = NhatKyTrangThaiYeuCau.rejected(
                                aggregate.request(), aggregate.driver(), RideRequestDecisionMother.DECISION_AT);
                var notification = ThongBao.bookingRejected(aggregate.request());

                assertThat(event.getSequence()).isEqualTo(2L);
                assertThat(event.getTrangThaiTruoc()).isEqualTo(TrangThaiYeuCau.PENDING);
                assertThat(event.getTrangThaiSau()).isEqualTo(TrangThaiYeuCau.REJECTED);
                assertThat(notification.getLoaiThongBao()).isEqualTo(LoaiThongBao.BOOKING_REJECTED);
                assertThat(notification.getNguoiNhan()).isSameAs(aggregate.passenger());
                assertThat(notification.getDeduplicationKey())
                                .isEqualTo("BOOKING_REJECTED:501:REJECTED");
        }

        @Test
        void givenAcceptedRequest_whenCreatingAuditAndNotification_thenPassengerAndTransitionAreCorrect() {
                var aggregate = RideRequestDecisionMother.aggregate();
                aggregate.request().accept(RideRequestDecisionMother.DECISION_AT);

                var event = NhatKyTrangThaiYeuCau.accepted(
                                aggregate.request(), aggregate.driver(), RideRequestDecisionMother.DECISION_AT);
                var notification = ThongBao.bookingAccepted(aggregate.request());

                assertThat(event.getSequence()).isEqualTo(2L);
                assertThat(event.getTrangThaiTruoc()).isEqualTo(TrangThaiYeuCau.PENDING);
                assertThat(event.getTrangThaiSau()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
                assertThat(event.getActor()).isSameAs(aggregate.driver());
                assertThat(notification.getLoaiThongBao()).isEqualTo(LoaiThongBao.BOOKING_ACCEPTED);
                assertThat(notification.getNguoiNhan()).isSameAs(aggregate.passenger());
                assertThat(notification.getDeduplicationKey())
                                .isEqualTo("BOOKING_ACCEPTED:501:ACCEPTED");
        }
}
