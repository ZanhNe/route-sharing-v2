package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.SafetyIncidentQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyIncidentRepository;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.security.ClientRequestInfo;
import com.zanh.route_sharing.service.SafetyIncidentQueryService;
import com.zanh.route_sharing.service.tripsafety.SafetyIncidentQueryResponseMapper;
import com.zanh.route_sharing.service.tripsafety.TripSafetyIncidentResponseMapper;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Service
public class SafetyIncidentQueryServiceImpl implements SafetyIncidentQueryService {
    private static final Set<String> OWNERSHIP = Set.of("ALL", "UNASSIGNED", "MINE");
    private final TripSafetyIncidentRepository e604Repository;
    private final TripSafetyIncidentResponseMapper e604Mapper;
    private final SafetyIncidentQueryRepository repository;
    private final SafetyIncidentQueryResponseMapper mapper;
    private final Clock clock;

    public SafetyIncidentQueryServiceImpl(TripSafetyIncidentRepository e604Repository,
            TripSafetyIncidentResponseMapper e604Mapper,
            SafetyIncidentQueryRepository repository,
            SafetyIncidentQueryResponseMapper mapper,
            Clock clock) {
        this.e604Repository = e604Repository;
        this.e604Mapper = e604Mapper;
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public SafetyIncidentSummaryResponse getSummary(Long actorId, Long incidentId) {
        validateActorIncident(actorId, incidentId);
        Instant now = TimePolicy.now(clock);
        return e604Mapper
                .toSummaryResponse(e604Repository.findAuthorizedSummary(actorId, incidentId, businessDate(now)));
    }

    @Override
    public SafetyIncidentQueueResponse getQueue(Long actorId, Long schoolId, TrangThaiXuLySuCo status,
            MucDoSuCo severity,
            String ownership, int page, int size) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (schoolId != null && schoolId <= 0)
            throw validation("schoolId phải là số dương.");
        validatePage(page, size);
        String normalized = ownership == null || ownership.isBlank() ? "ALL" : ownership.trim().toUpperCase();
        if (!OWNERSHIP.contains(normalized))
            throw validation("ownership không hợp lệ.");
        Instant now = TimePolicy.now(clock);
        return mapper.toQueue(
                repository.findQueue(actorId, schoolId, status, severity, normalized, page, size, businessDate(now)),
                page, size);
    }

    @Override
    public SafetyIncidentCaseResponse getCase(Long actorId, Long incidentId, ClientRequestInfo client) {
        validateActorIncident(actorId, incidentId);
        Instant now = TimePolicy.now(clock);
        return mapper.toCase(repository.findCase(actorId, incidentId, now, businessDate(now), client));
    }

    @Override
    public SafetyInvestigationContextResponse getInvestigationContext(Long actorId, Long incidentId,
            ClientRequestInfo client) {
        validateActorIncident(actorId, incidentId);
        Instant now = TimePolicy.now(clock);
        return mapper.toInvestigation(
                repository.findInvestigationContext(actorId, incidentId, now, businessDate(now), client));
    }

    @Override
    public SafetyLocationEvidenceResponse getLocationEvidence(Long actorId, Long incidentId, Instant from, Instant to,
            int page, int size, ClientRequestInfo client) {
        validateActorIncident(actorId, incidentId);
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to))
            throw validation("from không được sau to.");
        Instant now = TimePolicy.now(clock);
        return mapper.toLocations(repository.findLocationEvidence(actorId, incidentId, from, to, page, size, now,
                businessDate(now), client), page, size);
    }

    @Override
    public SafetyEligibleHandlersResponse getEligibleHandlers(Long actorId, Long incidentId, int page, int size) {
        validateActorIncident(actorId, incidentId);
        validatePage(page, size);
        Instant now = TimePolicy.now(clock);
        return mapper.toEligible(repository.findEligibleHandlers(actorId, incidentId, page, size, businessDate(now)),
                page, size);
    }

    private static void validateActorIncident(Long actorId, Long incidentId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (incidentId == null || incidentId <= 0)
            throw validation("incidentId phải là số dương.");
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50)
            throw validation("page/size không hợp lệ.");
    }

    private static LocalDate businessDate(Instant now) {
        return LocalDate.ofInstant(now, TimePolicy.BUSINESS_ZONE);
    }

    private static BusinessException validation(String m) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", m);
    }
}
