package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.util.Objects;
import java.util.Optional;

public record PreviewEvaluation(
        PreviewEvaluationStatus status,
        SharedRoutePreviewPreparation preparation) {

    public PreviewEvaluation {
        Objects.requireNonNull(status, "status không được trống");
        if (status == PreviewEvaluationStatus.ELIGIBLE && preparation == null) {
            throw new IllegalArgumentException("Đánh giá ELIGIBLE yêu cầu preparation");
        }
        if (status != PreviewEvaluationStatus.ELIGIBLE && preparation != null) {
            throw new IllegalArgumentException("Đánh giá ineligible không được chứa preparation");
        }
    }

    public static PreviewEvaluation eligible(SharedRoutePreviewPreparation preparation) {
        return new PreviewEvaluation(PreviewEvaluationStatus.ELIGIBLE,
                Objects.requireNonNull(preparation));
    }

    public static PreviewEvaluation ineligible(PreviewEvaluationStatus status) {
        if (status == PreviewEvaluationStatus.ELIGIBLE) {
            throw new IllegalArgumentException("Sử dụng eligible() cho trạng thái ELIGIBLE");
        }
        return new PreviewEvaluation(status, null);
    }

    public Optional<SharedRoutePreviewPreparation> optionalPreparation() {
        return Optional.ofNullable(preparation);
    }

    public SharedRoutePreviewPreparation requirePreparation() {
        if (preparation == null) {
            throw new IllegalStateException("Đánh giá không chứa preparation");
        }
        return preparation;
    }
}
