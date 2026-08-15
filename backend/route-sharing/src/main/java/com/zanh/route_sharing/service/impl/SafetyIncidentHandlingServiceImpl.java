package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.SafetyIncidentHandlingRepository;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.SafetyIncidentHandlingService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.tripsafety.SafetyIncidentHandlingResponseMapper;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class SafetyIncidentHandlingServiceImpl implements SafetyIncidentHandlingService {
    private final SafetyIncidentHandlingRepository repository;
    private final SafetyIncidentHandlingResponseMapper mapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final Clock clock;

    public SafetyIncidentHandlingServiceImpl(SafetyIncidentHandlingRepository repository,
            SafetyIncidentHandlingResponseMapper mapper,
            UserRealtimeEventPublisher realtimePublisher,
            Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Override
    public SafetyIncidentHandlingResponse claim(Long actorId, Long incidentId) {
        validateIds(actorId, incidentId);
        Instant now = TimePolicy.now(clock);
        var result = repository.claim(actorId, incidentId, now, businessDate(now));
        publish(result);
        return mapper.toResponse(result);
    }

    @Override
    public SafetyIncidentHandlingResponse investigate(Long actorId, Long incidentId) {
        validateIds(actorId, incidentId);
        Instant now = TimePolicy.now(clock);
        var result = repository.investigate(actorId, incidentId, now, businessDate(now));
        publish(result);
        return mapper.toResponse(result);
    }

    @Override
    public SafetyIncidentHandlingResponse reassign(Long actorId, Long incidentId,
            SafetyIncidentReassignRequest request) {
        validateIds(actorId, incidentId);
        if (request == null || request.newHandlerUserId() == null || request.newHandlerUserId() <= 0
                || request.reason() == null || request.reason().trim().isEmpty()) {
            throw validation("Dữ liệu chuyển người xử lý không hợp lệ.");
        }
        Instant now = TimePolicy.now(clock);
        var result = repository.reassign(actorId, incidentId, request.newHandlerUserId(), request.reason().trim(), now,
                businessDate(now));
        publish(result);
        return mapper.toResponse(result);
    }

    @Override
    public SafetyIncidentHandlingResponse finalizeIncident(Long actorId, Long incidentId,
            SafetyIncidentFinalizeRequest request) {
        validateIds(actorId, incidentId);
        if (request == null || request.outcome() == null || request.safeConclusion() == null
                || request.safeConclusion().trim().isEmpty()) {
            throw validation("Dữ liệu kết thúc xử lý incident không hợp lệ.");
        }
        TrangThaiXuLySuCo outcome;
        try {
            outcome = TrangThaiXuLySuCo.valueOf(request.outcome().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INCIDENT_OUTCOME",
                    "outcome phải là RESOLVED hoặc FALSE_ALARM.");
        }
        if (outcome != TrangThaiXuLySuCo.RESOLVED && outcome != TrangThaiXuLySuCo.FALSE_ALARM) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INCIDENT_OUTCOME",
                    "outcome phải là RESOLVED hoặc FALSE_ALARM.");
        }
        Instant now = TimePolicy.now(clock);
        var result = repository.finalizeIncident(actorId, incidentId, outcome, request.safeConclusion().trim(), now,
                businessDate(now));
        publish(result);
        return mapper.toResponse(result);
    }

    private void publish(
            com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentHandlingCommitResult r) {
        if (!r.changed())
            return;
        var workEvent = RealtimeNotificationEventFactory.tripSafetyIncidentWorkChanged(
                r.incidentId(), r.tripId(), r.changeType(), r.status(), r.primaryHandlerUserId(), r.changedAt());
        r.safetyRealtimeRecipientUserIds().forEach(id -> realtimePublisher.publish(id, workEvent));

        if (r.reporterUserId() != null && !"REASSIGNED".equals(r.changeType())) {
            var reporterEvent = RealtimeNotificationEventFactory.tripSafetyIncidentStatusChanged(
                    r.incidentId(), r.tripId(), r.status(), r.changedAt());
            realtimePublisher.publish(r.reporterUserId(), reporterEvent);
        }
    }

    private static void validateIds(Long actorId, Long incidentId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (incidentId == null || incidentId <= 0)
            throw validation("incidentId phải là số dương.");
    }

    private static LocalDate businessDate(Instant now) {
        return LocalDate.ofInstant(now, TimePolicy.BUSINESS_ZONE);
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
