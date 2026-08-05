package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;
import java.util.Objects;

public record RideRequestEvaluation(
        RideRequestEvaluationStatus status,
        RideRequestPreparation preparation,
        Long existingRideRequestId,
        TrangThaiYeuCau existingStatus,
        Instant cooldownUntil) {

    public RideRequestEvaluation {
        Objects.requireNonNull(status, "status không được trống");
        if (status == RideRequestEvaluationStatus.ELIGIBLE && preparation == null) {
            throw new IllegalArgumentException("ELIGIBLE yêu cầu preparation");
        }
        if (status != RideRequestEvaluationStatus.ELIGIBLE && preparation != null) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ không được chứa preparation");
        }
        if (status == RideRequestEvaluationStatus.UNFINISHED_REQUEST_EXISTS
                && (existingRideRequestId == null || existingStatus == null)) {
            throw new IllegalArgumentException("Kết quả blocking phải chứa yêu cầu hiện có");
        }
        if (status == RideRequestEvaluationStatus.REJECTION_COOLDOWN_ACTIVE
                && cooldownUntil == null) {
            throw new IllegalArgumentException("Kết quả cooldown phải chứa cooldownUntil");
        }
    }

    public static RideRequestEvaluation eligible(RideRequestPreparation preparation) {
        return new RideRequestEvaluation(
                RideRequestEvaluationStatus.ELIGIBLE,
                Objects.requireNonNull(preparation),
                null,
                null,
                null);
    }

    public static RideRequestEvaluation ineligible(RideRequestEvaluationStatus status) {
        if (status == RideRequestEvaluationStatus.ELIGIBLE
                || status == RideRequestEvaluationStatus.UNFINISHED_REQUEST_EXISTS
                || status == RideRequestEvaluationStatus.REJECTION_COOLDOWN_ACTIVE) {
            throw new IllegalArgumentException("Phải dùng factory chuyên biệt cho " + status);
        }
        return new RideRequestEvaluation(status, null, null, null, null);
    }

    public static RideRequestEvaluation unfinished(
            Long rideRequestId,
            TrangThaiYeuCau existingStatus) {
        return new RideRequestEvaluation(
                RideRequestEvaluationStatus.UNFINISHED_REQUEST_EXISTS,
                null,
                rideRequestId,
                existingStatus,
                null);
    }

    public static RideRequestEvaluation cooldown(Instant cooldownUntil) {
        return new RideRequestEvaluation(
                RideRequestEvaluationStatus.REJECTION_COOLDOWN_ACTIVE,
                null,
                null,
                null,
                cooldownUntil);
    }

    public RideRequestPreparation requirePreparation() {
        if (preparation == null) {
            throw new IllegalStateException("Evaluation không chứa preparation");
        }
        return preparation;
    }
}
