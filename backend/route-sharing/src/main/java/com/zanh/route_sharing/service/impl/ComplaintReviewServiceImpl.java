package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.complaint.review.*;
import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.review.ComplaintReviewRepository;
import com.zanh.route_sharing.repository.complaint.review.model.ComplaintReviewSnapshots;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.ComplaintReviewService;
import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class ComplaintReviewServiceImpl implements ComplaintReviewService {
    private final ComplaintReviewRepository repository;
    private final Clock clock;

    public ComplaintReviewServiceImpl(ComplaintReviewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public ComplaintReviewQueueResponse queue(Long actorId, String status, int page, int size) {
        requireActor(actorId);
        requirePage(page, size);
        var result = repository.findQueue(actorId, status, page, size, businessDate());
        return new ComplaintReviewQueueResponse(result.items().stream().map(i -> new ComplaintReviewQueueResponse.Item(
                i.complaintId(), i.status(), i.submittedAt(), i.tripId(), i.rideRequestId(), i.complainantRole(),
                i.respondentRole(), i.currentReviewerId(), i.reviewStartedAt(), i.responseDeadline(),
                i.evidenceRequestDeadline(), i.resolvedAt())).toList(),
                PageMeta.of(page, size, result.totalElements()));
    }

    @Override
    public ComplaintReviewCaseResponse reviewerCase(Long actorId, Long complaintId) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        ComplaintReviewSnapshots.ReviewCase c = repository.findReviewerCase(actorId, complaintId, businessDate());
        return new ComplaintReviewCaseResponse(c.complaintId(), c.status(), c.title(), c.allegation(), c.submittedAt(),
                c.tripId(), c.rideRequestId(), c.complainantId(), c.respondentId(), c.currentReviewerId(),
                c.reviewStartedAt(),
                c.appliedResponseWindowHours(), c.responseDeadline(), c.requestedParticipantId(), c.requestReason(),
                c.evidenceRequestAt(), c.evidenceRequestDeadline(), c.evidenceIdWatermark(), c.finalConclusion(),
                c.resolvedAt(),
                c.respondentResponse(), c.respondentResponseAt(), c.complainantEvidenceCount(),
                c.respondentEvidenceCount(),
                c.history().stream()
                        .map(h -> new ComplaintReviewCaseResponse.HistoryItem(h.sequence(), h.action(),
                                h.previousState(),
                                h.resultingState(), h.previousReviewerId(), h.resultingReviewerId(), h.actorId(),
                                h.targetParticipantId(),
                                h.occurredAt(), h.deadlineAt(), h.evidenceIdWatermark(), h.reason()))
                        .toList());
    }

    @Override
    public ComplaintReviewActionResponse claim(Long actorId, Long complaintId) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        Instant commandTime = now();
        return action(repository.claim(actorId, complaintId, commandTime, businessDate(commandTime)));
    }

    @Override
    public EligibleReviewerPageResponse eligibleReviewers(Long actorId, Long complaintId, int page, int size) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        requirePage(page, size);
        var result = repository.findEligibleReviewers(actorId, complaintId, page, size, businessDate());
        return new EligibleReviewerPageResponse(result.items().stream()
                .map(i -> new EligibleReviewerPageResponse.Item(i.reviewerId(), i.displayName())).toList(),
                PageMeta.of(page, size, result.totalElements()));
    }

    @Override
    public ComplaintReviewActionResponse reassign(Long actorId, Long complaintId, ComplaintReassignRequest request) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        if (request == null)
            throw validation("request không được trống.");
        Instant commandTime = now();
        return action(repository.reassign(actorId, complaintId, request.newReviewerId(), request.reason(), commandTime,
                businessDate(commandTime)));
    }

    @Override
    public ComplaintParticipantReviewResponse participantView(Long actorId, Long complaintId) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        var c = repository.findParticipantView(actorId, complaintId);
        return new ComplaintParticipantReviewResponse(c.complaintId(), c.status(), c.title(), c.allegation(),
                c.submittedAt(),
                c.tripId(), c.rideRequestId(), c.actorRole(), c.reviewStartedAt(), c.responseDeadline(),
                c.requestedParticipantId(), c.requestReason(), c.evidenceRequestDeadline(), c.conclusion(),
                c.resolvedAt(),
                c.ownResponse(), c.ownResponseAt(), c.ownEvidenceCount());
    }

    @Override
    public ComplaintFormalResponseResponse respond(Long actorId, Long complaintId, ComplaintResponseRequest request) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        if (request == null)
            throw validation("request không được trống.");
        var r = repository.submitResponse(actorId, complaintId, request.content(), now());
        return new ComplaintFormalResponseResponse(r.complaintId(), r.responseId(), r.content(), r.submittedAt(),
                r.created());
    }

    @Override
    public ComplaintReviewActionResponse requestMoreEvidence(Long actorId, Long complaintId,
            ComplaintEvidenceRequest request) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        if (request == null)
            throw validation("request không được trống.");
        Instant commandTime = now();
        return action(repository.requestMoreEvidence(actorId, complaintId, request.targetParticipantId(),
                request.reason(), commandTime, businessDate(commandTime)));
    }

    @Override
    public ComplaintReviewActionResponse resume(Long actorId, Long complaintId) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        Instant commandTime = now();
        return action(repository.resume(actorId, complaintId, commandTime, businessDate(commandTime)));
    }

    @Override
    public ComplaintReviewActionResponse finalizeReview(Long actorId, Long complaintId,
            ComplaintReviewDecisionRequest request) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        if (request == null)
            throw validation("request không được trống.");
        String conclusion = normalizeSafeConclusion(request.conclusion());
        Instant commandTime = now();
        return action(repository.finalizeReview(actorId, complaintId, request.outcome(), conclusion, commandTime,
                businessDate(commandTime)));
    }

    @Override
    public ComplaintInvestigationContextResponse investigation(Long actorId, Long complaintId, String ip,
            String userAgent) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        Instant readTime = now();
        var x = repository.findInvestigationContext(actorId, complaintId, businessDate(readTime), ip, userAgent,
                readTime);
        return new ComplaintInvestigationContextResponse(x.complaintId(), x.tripId(), x.tripStatus(), x.rideRequestId(),
                x.rideRequestStatus(), x.driverId(), x.passengerId(), x.stopCount(), x.linkedIncidentId(),
                x.linkedIncidentType(),
                x.linkedIncidentStatus(), x.tripEndedAt());
    }

    @Override
    public ComplaintLocationEvidencePageResponse locations(Long actorId, Long complaintId, int page, int size,
            String ip, String userAgent) {
        requireActor(actorId);
        requirePositive(complaintId, "complaintId");
        if (page < 0 || size < 1 || size > 100)
            throw validation("location pagination không hợp lệ.");
        Instant readTime = now();
        var result = repository.findLocationEvidence(actorId, complaintId, page, size, businessDate(readTime), ip,
                userAgent, readTime);
        return new ComplaintLocationEvidencePageResponse(
                result.items().stream().map(l -> new ComplaintLocationEvidencePageResponse.Item(
                        l.locationId(), l.sequence(), l.observedAt(), l.receivedAt(), l.latitude(), l.longitude(),
                        l.accuracyMeters())).toList(),
                PageMeta.of(page, size, result.totalElements()));
    }

    private ComplaintReviewActionResponse action(ComplaintReviewSnapshots.Action a) {
        return new ComplaintReviewActionResponse(a.complaintId(), a.status(), a.currentReviewerId(),
                a.reviewStartedAt(),
                a.responseDeadline(), a.evidenceRequestDeadline(), a.resolvedAt(), a.changed());
    }

    private Instant now() {
        return TimePolicy.now(clock);
    }

    private LocalDate businessDate() {
        return businessDate(now());
    }

    private static LocalDate businessDate(Instant instant) {
        return LocalDate.ofInstant(instant, TimePolicy.BUSINESS_ZONE);
    }

    private static void requireActor(Long actorId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0)
            throw validation(field + " phải là số dương.");
    }

    private static void requirePage(int page, int size) {
        if (!PaginationPolicy.isValid(page, size))
            throw validation("Thông tin phân trang không hợp lệ.");
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private static String normalizeSafeConclusion(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty() || normalized.length() > 5000)
            throw validation("conclusion không hợp lệ.");
        boolean unsafeControl = normalized.codePoints()
                .anyMatch(cp -> Character.isISOControl(cp) && cp != '\n' && cp != '\t');
        if (unsafeControl)
            throw validation("conclusion chứa ký tự điều khiển không hợp lệ.");
        return normalized;
    }
}
