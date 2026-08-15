package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyIncidentRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyInterventionRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.SafetyIncidentQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommand;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripSafetyIncidentService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.tripsafety.TripSafetyIncidentOperationResult;
import com.zanh.route_sharing.service.tripsafety.TripSafetyIncidentResponseMapper;
import com.zanh.route_sharing.service.tripsafety.SafetyIncidentQueryResponseMapper;
import com.zanh.route_sharing.dto.trip.safety.ReporterSafetyIncidentStatusResponse;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
public class TripSafetyIncidentServiceImpl implements TripSafetyIncidentService {
    private static final Set<LoaiSuCo> REPORTABLE_TYPES = Set.of(
            LoaiSuCo.ROUTE_DEVIATION,
            LoaiSuCo.TECHNICAL_INCIDENT,
            LoaiSuCo.HARASSMENT_REPORT,
            LoaiSuCo.OTHER,
            LoaiSuCo.SOS);

    private final TripSafetyIncidentRepository repository;
    private final TripSafetyInterventionRepository interventionRepository;
    private final TripSafetyIncidentResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final SafetyIncidentQueryRepository safetyQueryRepository;
    private final SafetyIncidentQueryResponseMapper safetyQueryMapper;
    private final Clock clock;

    public TripSafetyIncidentServiceImpl(
            TripSafetyIncidentRepository repository,
            TripSafetyInterventionRepository interventionRepository,
            TripSafetyIncidentResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimePublisher,
            SafetyIncidentQueryRepository safetyQueryRepository,
            SafetyIncidentQueryResponseMapper safetyQueryMapper,
            Clock clock) {
        this.repository = repository;
        this.interventionRepository = interventionRepository;
        this.responseMapper = responseMapper;
        this.realtimePublisher = realtimePublisher;
        this.safetyQueryRepository = safetyQueryRepository;
        this.safetyQueryMapper = safetyQueryMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TripSafetyIncidentOperationResult report(Long actorId, Long tripId, TripSafetyIncidentRequest request) {
        validate(actorId, tripId, request);
        String description = normalizeDescription(request.type(), request.description());
        Instant occurredAt = TimePolicy.now(clock);
        var incidentCommitted = repository.commit(new TripSafetyIncidentCommand(
                actorId,
                tripId,
                request.type(),
                description,
                request.reportedParticipantId(),
                occurredAt));
        var containment = request.type() == LoaiSuCo.SOS
                ? interventionRepository.ensureInitialContainment(actorId, incidentCommitted.incidentId(), occurredAt)
                : null;
        var committed = containment == null
                ? incidentCommitted
                : new com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommitResult(
                        incidentCommitted.incidentId(), incidentCommitted.tripId(), incidentCommitted.type(),
                        incidentCommitted.severity(), incidentCommitted.status(), incidentCommitted.reporterSource(),
                        incidentCommitted.reportedAt(), incidentCommitted.createdNew(),
                        incidentCommitted.realtimeRecipientUserIds(),
                        containment.snapshot(), containment.changed(), containment.tripStateRealtimeRecipientUserIds(),
                        containment.participantRealtimeRecipientUserIds());

        if (committed.createdNew()) {
            var event = RealtimeNotificationEventFactory.tripSafetyIncidentReported(
                    committed.incidentId(), committed.tripId(), committed.type().name(), committed.severity().name(),
                    committed.status().name(), committed.reportedAt());
            committed.realtimeRecipientUserIds().forEach(recipientId -> realtimePublisher.publish(recipientId, event));
        }
        if (committed.interventionChanged() && committed.intervention() != null) {
            var i = committed.intervention();
            if (!committed.tripStateRealtimeRecipientUserIds().isEmpty()) {
                var event = RealtimeNotificationEventFactory.tripSafetyStateChanged(i.tripId(), i.interventionId(),
                        i.changeType(), i.tripStatus(), i.changedAt());
                committed.tripStateRealtimeRecipientUserIds().forEach(id -> realtimePublisher.publish(id, event));
            }
            if (!committed.participantRealtimeRecipientUserIds().isEmpty() && i.targetRideRequestId() != null) {
                var event = RealtimeNotificationEventFactory.tripParticipantSafetyChanged(i.tripId(),
                        i.interventionId(),
                        i.targetRideRequestId(), i.targetBookingStatus(), i.changedAt());
                committed.participantRealtimeRecipientUserIds().forEach(id -> realtimePublisher.publish(id, event));
            }
        }

        return new TripSafetyIncidentOperationResult(
                responseMapper.toReportResponse(committed),
                committed.createdNew());
    }

    @Override
    public ReporterSafetyIncidentStatusResponse getOwnIncidentStatus(Long actorId, Long tripId, Long incidentId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0 || incidentId == null || incidentId <= 0) {
            throw validation("tripId/incidentId phải là số dương.");
        }
        return safetyQueryMapper
                .toReporterStatus(safetyQueryRepository.findReporterStatus(actorId, tripId, incidentId));
    }

    private static void validate(Long actorId, Long tripId, TripSafetyIncidentRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0 || request == null || request.type() == null) {
            throw validation("Dữ liệu báo sự cố không hợp lệ.");
        }
        if (!REPORTABLE_TYPES.contains(request.type())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INCIDENT_TYPE_NOT_REPORTABLE",
                    "Loại sự cố không thuộc phạm vi báo cáo thủ công của E6-04.");
        }
        if (request.reportedParticipantId() != null && request.reportedParticipantId() <= 0) {
            throw validation("reportedParticipantId phải là số dương.");
        }
        if (request.type() == LoaiSuCo.SOS && !Boolean.TRUE.equals(request.emergencyActionConfirmed())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EMERGENCY_ACTION_CONFIRMATION_REQUIRED",
                    "SOS yêu cầu emergencyActionConfirmed=true vì có thể thay đổi lifecycle Trip/Passenger.");
        }
    }

    private static String normalizeDescription(LoaiSuCo type, String description) {
        String normalized = description == null ? null : description.trim();
        if (normalized != null && normalized.length() > 5000) {
            throw validation("description không được vượt quá 5000 ký tự.");
        }
        if (type != LoaiSuCo.SOS && (normalized == null || normalized.isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INCIDENT_DESCRIPTION_REQUIRED",
                    "Sự cố thông thường phải có mô tả.");
        }
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
