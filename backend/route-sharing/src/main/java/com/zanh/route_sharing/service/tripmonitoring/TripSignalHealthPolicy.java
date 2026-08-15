package com.zanh.route_sharing.service.tripmonitoring;

import com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class TripSignalHealthPolicy {

    public TripSignalMonitoringDecision evaluate(
            TrangThaiGiamSatChuyenDi currentState,
            Instant signalReferenceAt,
            Instant evaluationAt,
            long submissionCadenceSeconds,
            long delayThresholdSeconds,
            long lostThresholdSeconds,
            Instant latestTransitionSignalReferenceAt) {
        Objects.requireNonNull(currentState, "currentState không được trống");
        Objects.requireNonNull(signalReferenceAt, "signalReferenceAt không được trống");
        Objects.requireNonNull(evaluationAt, "evaluationAt không được trống");
        if (signalReferenceAt.isAfter(evaluationAt)) {
            throw new IllegalArgumentException("signalReferenceAt không được sau evaluationAt.");
        }
        if (submissionCadenceSeconds <= 0
                || delayThresholdSeconds <= submissionCadenceSeconds
                || lostThresholdSeconds <= delayThresholdSeconds) {
            throw new IllegalArgumentException("Monitoring configuration không hợp lệ.");
        }

        long signalAgeSeconds = Duration.between(signalReferenceAt, evaluationAt).getSeconds();
        TrangThaiGiamSatChuyenDi classified = classify(signalAgeSeconds, delayThresholdSeconds, lostThresholdSeconds);
        if (classified == currentState) {
            return TripSignalMonitoringDecision.unchanged(currentState);
        }

        int currentSeverity = severity(currentState);
        int classifiedSeverity = severity(classified);
        if (classifiedSeverity > currentSeverity) {
            return new TripSignalMonitoringDecision(
                    currentState,
                    classified,
                    true,
                    classified == TrangThaiGiamSatChuyenDi.SIGNAL_LOST
                            ? TripSignalMonitoringReason.SIGNAL_LOST_THRESHOLD_EXCEEDED
                            : TripSignalMonitoringReason.SIGNAL_DELAY_THRESHOLD_EXCEEDED);
        }

        if (latestTransitionSignalReferenceAt == null
                || !signalReferenceAt.isAfter(latestTransitionSignalReferenceAt)) {
            return TripSignalMonitoringDecision.unchanged(currentState);
        }

        return new TripSignalMonitoringDecision(
                currentState,
                classified,
                true,
                classified == TrangThaiGiamSatChuyenDi.NORMAL
                        ? TripSignalMonitoringReason.FRESH_SIGNAL_RECOVERED
                        : TripSignalMonitoringReason.FRESH_SIGNAL_RECLASSIFIED);
    }

    private static TrangThaiGiamSatChuyenDi classify(
            long signalAgeSeconds,
            long delayThresholdSeconds,
            long lostThresholdSeconds) {
        if (signalAgeSeconds >= lostThresholdSeconds) {
            return TrangThaiGiamSatChuyenDi.SIGNAL_LOST;
        }
        if (signalAgeSeconds >= delayThresholdSeconds) {
            return TrangThaiGiamSatChuyenDi.SIGNAL_DELAYED;
        }
        return TrangThaiGiamSatChuyenDi.NORMAL;
    }

    private static int severity(TrangThaiGiamSatChuyenDi state) {
        return switch (state) {
            case NORMAL -> 0;
            case SIGNAL_DELAYED -> 1;
            case SIGNAL_LOST -> 2;
        };
    }
}
