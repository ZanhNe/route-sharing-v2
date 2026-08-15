package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.NhatKyGiamSatTinHieu;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.tripmonitoring.TripSignalMonitoringRepository;
import com.zanh.route_sharing.service.TripSignalMonitoringService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;
import com.zanh.route_sharing.service.tripmonitoring.TripSignalHealthPolicy;
import com.zanh.route_sharing.service.tripmonitoring.TripSignalMonitoringDecision;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class TripSignalMonitoringServiceImpl implements TripSignalMonitoringService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripSignalMonitoringServiceImpl.class);
    private static final Set<TrangThaiVanHanhChuyenDi> TRACKING_ACTIVE_STATES = Set.of(
            TrangThaiVanHanhChuyenDi.IN_PROGRESS,
            TrangThaiVanHanhChuyenDi.SECURITY_FROZEN);

    private final TripSignalMonitoringRepository repository;
    private final TripSignalHealthPolicy policy;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public TripSignalMonitoringServiceImpl(
            TripSignalMonitoringRepository repository,
            TripSignalHealthPolicy policy,
            UserRealtimeEventPublisher realtimePublisher,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.repository = repository;
        this.policy = policy;
        this.realtimePublisher = realtimePublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public void evaluateTrackingActiveTrips() {
        List<Long> tripIds = repository.findTrackingActiveTripIds();
        for (Long tripId : tripIds) {
            try {
                CommittedTransition committed = transactionTemplate.execute(status -> evaluateOneTrip(tripId));
                publishCommitted(committed);
            } catch (RuntimeException exception) {
                LOGGER.warn("Trip signal monitoring evaluation failed: tripId={}", tripId, exception);
            }
        }
    }

    private CommittedTransition evaluateOneTrip(Long tripId) {
        ChuyenDi trip = repository.lockTrip(tripId);
        if (!TRACKING_ACTIVE_STATES.contains(trip.getTrangThaiVanHanh()) || trip.getBatDauLuc() == null) {
            return null;
        }

        Instant evaluationAt = TimePolicy.now(clock);
        Instant signalReferenceAt = trip.getNhanTinHieuCuoiLuc() != null
                ? trip.getNhanTinHieuCuoiLuc()
                : trip.getBatDauLuc();
        if (signalReferenceAt == null || signalReferenceAt.isAfter(evaluationAt)) {
            throw new IllegalStateException("Trip signal reference invariant không hợp lệ.");
        }

        CauHinhNghiepVu config = repository.loadCurrentConfiguration(tripId);
        long cadence = requirePositive(config.getChuKyGuiViTriGiay(), "chuKyGuiViTriGiay");
        long delay = requirePositive(config.getThoiGianTreTinHieuGiay(), "thoiGianTreTinHieuGiay");
        long lost = requirePositive(config.getThoiGianMatTinHieuGiay(), "thoiGianMatTinHieuGiay");

        NhatKyGiamSatTinHieu latest = repository.findLatestTransition(tripId).orElse(null);
        Instant latestTransitionReference = latest == null ? null : latest.getSignalReferenceAt();
        if (trip.getTrangThaiGiamSat() != com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi.NORMAL
                && latest == null) {
            throw new IllegalStateException("Degraded monitoring state phải có transition history.");
        }

        TripSignalMonitoringDecision decision = policy.evaluate(
                trip.getTrangThaiGiamSat(),
                signalReferenceAt,
                evaluationAt,
                cadence,
                delay,
                lost,
                latestTransitionReference);
        if (!decision.changed()) {
            return null;
        }

        var previousState = trip.getTrangThaiGiamSat();
        trip.transitionMonitoringState(decision.desiredState());
        long sequence = repository.nextTransitionSequence(tripId);
        repository.persistTransition(NhatKyGiamSatTinHieu.transitioned(
                trip,
                sequence,
                previousState,
                decision.desiredState(),
                evaluationAt,
                signalReferenceAt,
                delay,
                lost,
                decision.reason().name()));
        List<Long> recipients = repository.findRealtimeRecipientUserIds(
                tripId,
                TrangThaiYeuCau.activeTripParticipantStates());
        repository.flush();
        return new CommittedTransition(
                tripId,
                previousState,
                decision.desiredState(),
                signalReferenceAt,
                evaluationAt,
                recipients);
    }

    private void publishCommitted(CommittedTransition committed) {
        if (committed == null) {
            return;
        }
        RealtimeEventEnvelope<?> event = RealtimeNotificationEventFactory.tripSignalMonitoringChanged(
                committed.tripId(),
                committed.previousState(),
                committed.monitoringState(),
                committed.signalReferenceAt(),
                committed.changedAt());
        String destination = "/queue/trips/" + committed.tripId() + "/events";
        committed.recipientUserIds().stream().distinct()
                .forEach(recipientUserId -> realtimePublisher.publish(recipientUserId, destination, event));
    }

    private static long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(name + " phải là số dương.");
        }
        return value;
    }

    private record CommittedTransition(
            Long tripId,
            com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi previousState,
            com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi monitoringState,
            Instant signalReferenceAt,
            Instant changedAt,
            List<Long> recipientUserIds) {
    }
}
