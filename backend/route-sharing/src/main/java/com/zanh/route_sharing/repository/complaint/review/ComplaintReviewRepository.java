package com.zanh.route_sharing.repository.complaint.review;

import com.zanh.route_sharing.domain.entity.KhieuNai;
import com.zanh.route_sharing.domain.enums.LoaiTaiNguyenNhayCam;
import com.zanh.route_sharing.repository.complaint.review.model.ComplaintReviewSnapshots;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface ComplaintReviewRepository {
        ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.QueueItem> findQueue(Long actorId, String status,
                        int page, int size, LocalDate date);

        ComplaintReviewSnapshots.ReviewCase findReviewerCase(Long actorId, Long complaintId, LocalDate date);

        ComplaintReviewSnapshots.Action claim(Long actorId, Long complaintId, Instant now, LocalDate date);

        ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.EligibleReviewer> findEligibleReviewers(Long actorId,
                        Long complaintId, int page, int size, LocalDate date);

        ComplaintReviewSnapshots.Action reassign(Long actorId, Long complaintId, Long newReviewerId, String reason,
                        Instant now, LocalDate date);

        ComplaintReviewSnapshots.ParticipantView findParticipantView(Long actorId, Long complaintId);

        ComplaintReviewSnapshots.FormalResponse submitResponse(Long actorId, Long complaintId, String content,
                        Instant now);

        ComplaintReviewSnapshots.Action requestMoreEvidence(Long actorId, Long complaintId, Long targetParticipantId,
                        String reason, Instant now, LocalDate date);

        ComplaintReviewSnapshots.Action resume(Long actorId, Long complaintId, Instant now, LocalDate date);

        ComplaintReviewSnapshots.Action finalizeReview(Long actorId, Long complaintId, String outcome,
                        String conclusion, Instant now, LocalDate date);

        KhieuNai lockParticipantReviewComplaint(Long actorId, Long complaintId, Instant now);

        Optional<ComplaintReviewSnapshots.Evidence> findExistingEvidence(Long complaintId, Long uploaderId,
                        String fingerprint);

        ComplaintReviewSnapshots.Evidence persistReviewEvidence(KhieuNai complaint, Long uploaderId, String category,
                        String originalFilename, String mediaType, long sizeBytes, String fingerprint,
                        String storageKey,
                        String description, Instant uploadedAt);

        ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Evidence> findParticipantEvidence(Long actorId,
                        Long complaintId, int page, int size);

        Optional<ComplaintReviewSnapshots.Evidence> findParticipantEvidence(Long actorId, Long complaintId,
                        Long evidenceId);

        ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Evidence> findReviewerEvidence(Long actorId,
                        Long complaintId, int page, int size, LocalDate date);

        Optional<ComplaintReviewSnapshots.Evidence> findReviewerEvidence(Long actorId, Long complaintId,
                        Long evidenceId, LocalDate date);

        long maxEvidenceIdForParticipant(Long complaintId, Long participantId);

        ComplaintReviewSnapshots.Investigation findInvestigationContext(Long actorId, Long complaintId, LocalDate date,
                        String ip, String userAgent, Instant now);

        ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Location> findLocationEvidence(Long actorId,
                        Long complaintId,
                        int page, int size, LocalDate date, String ip, String userAgent, Instant now);

        void recordSensitiveRead(Long actorId, Long complaintId, Long resourceId, LoaiTaiNguyenNhayCam resourceType,
                        String purpose, String ip, String userAgent, Instant now, LocalDate date);
}
