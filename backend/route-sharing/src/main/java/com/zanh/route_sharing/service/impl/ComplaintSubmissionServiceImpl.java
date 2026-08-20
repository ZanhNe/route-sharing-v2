package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintRequest;
import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.submission.ComplaintSubmissionRepository;
import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionCommand;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.ComplaintSubmissionService;
import com.zanh.route_sharing.service.complaint.ComplaintSubmissionResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ComplaintSubmissionServiceImpl implements ComplaintSubmissionService {
    private final ComplaintSubmissionRepository repository;
    private final ComplaintSubmissionResponseMapper mapper;

    public ComplaintSubmissionServiceImpl(
            ComplaintSubmissionRepository repository,
            ComplaintSubmissionResponseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SubmitComplaintResponse submit(
            Long actorId,
            Long tripId,
            Long rideRequestId,
            SubmitComplaintRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0 || rideRequestId == null || rideRequestId <= 0 || request == null) {
            throw validation("Thông tin nộp khiếu nại không hợp lệ.");
        }
        if (request.incidentId() != null && request.incidentId() <= 0) {
            throw validation("incidentId phải là số dương.");
        }
        String title = normalize(request.title(), 5, 255, "title");
        String content = normalize(request.content(), 20, 5000, "content");
        return mapper.toResponse(repository.commit(new ComplaintSubmissionCommand(
                actorId, tripId, rideRequestId, title, content, request.incidentId())));
    }

    private static String normalize(String value, int min, int max, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.length() < min || normalized.length() > max) {
            throw validation(field + " không hợp lệ.");
        }
        return normalized;
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
