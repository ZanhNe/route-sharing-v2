package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.dto.membership.onboarding.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.membership.onboarding.*;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.MembershipOnboardingService;
import com.zanh.route_sharing.service.membership.onboarding.*;
import com.zanh.route_sharing.service.membership.onboarding.model.*;
import com.zanh.route_sharing.storage.evidence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class MembershipOnboardingServiceImpl implements MembershipOnboardingService {
    private final MembershipOnboardingContextRepository contextRepository;
    private final MembershipProfileRepository profileRepository;
    private final MembershipEvidenceRepository evidenceRepository;
    private final MembershipSubmissionRepository submissionRepository;
    private final MembershipOnboardingResponseMapper mapper;
    private final MembershipPolicy policy;
    private final MembershipEvidencePreparationService preparationService;
    private final MembershipDraftCoordinator draftCoordinator;
    private final MembershipSubmissionCoordinator submissionCoordinator;
    private final EvidenceBinaryStorage storage;

    public MembershipOnboardingServiceImpl(MembershipOnboardingContextRepository contextRepository,
            MembershipProfileRepository profileRepository, MembershipEvidenceRepository evidenceRepository,
            MembershipSubmissionRepository submissionRepository, MembershipOnboardingResponseMapper mapper,
            MembershipPolicy policy, MembershipEvidencePreparationService preparationService,
            MembershipDraftCoordinator draftCoordinator, MembershipSubmissionCoordinator submissionCoordinator,
            EvidenceBinaryStorage storage) {
        this.contextRepository = contextRepository;
        this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository;
        this.submissionRepository = submissionRepository;
        this.mapper = mapper;
        this.policy = policy;
        this.preparationService = preparationService;
        this.draftCoordinator = draftCoordinator;
        this.submissionCoordinator = submissionCoordinator;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipProfileResponse getCurrent(Long actorId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        var context = contextRepository.requireReadContext(actorId);
        policy.requireAccountState(context.account());
        HoSoThanhVien current = profileRepository.findCurrent(actorId, context.school().getId()).orElse(null);
        policy.requireStudentProfile(current);
        if (current == null)
            return mapper.notStarted(context.school());
        HoSoSinhVien profile = (HoSoSinhVien) current;
        List<BangChungThanhVien> evidence = evidenceRepository.findCurrent(profile.getId());
        LanNopHoSoThanhVien submission = submissionRepository.findInitial(profile.getId()).orElse(null);
        return mapper.profile(profile, evidence, submission);
    }

    @Override
    public MembershipDraftCommitResult saveDraft(Long actorId, MembershipProfileDraftRequest request,
            MultipartFile front, MultipartFile back, MultipartFile confirmation) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (request == null)
            throw validation("Thiếu dữ liệu hồ sơ.");
        List<PreparedMembershipEvidence> prepared = preparationService.prepare(front, back, confirmation);
        try {
            return draftCoordinator.commit(actorId, request, prepared);
        } finally {
            preparationService.cleanup(prepared);
        }
    }

    @Override
    public MembershipSubmissionResponse submit(Long actorId, MembershipProfileDraftRequest request,
            MultipartFile front, MultipartFile back, MultipartFile confirmation) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (request == null)
            throw validation("Thiếu dữ liệu hồ sơ.");
        List<PreparedMembershipEvidence> prepared = preparationService.prepare(front, back, confirmation);
        try {
            return submissionCoordinator.commit(actorId, request, prepared);
        } finally {
            preparationService.cleanup(prepared);
        }
    }

    @Override
    public MembershipEvidenceDownloadResult downloadEvidence(Long actorId, Long evidenceId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (evidenceId == null || evidenceId <= 0)
            throw validation("evidenceId phải là số dương.");
        var context = contextRepository.requireReadContext(actorId);
        policy.requireAccountState(context.account());
        var row = evidenceRepository.findOwnedBinary(actorId, evidenceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND",
                        "Không tìm thấy bằng chứng phù hợp."));
        try {
            VerifiedBinary verified = storage.verify(row.storageKey(), row.sizeBytes(), row.sha256());
            return new MembershipEvidenceDownloadResult(verified.resource(), row.originalFilename(),
                    row.verifiedMediaType(), row.sizeBytes());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_STORAGE_INTEGRITY_VIOLATION",
                    "Bằng chứng lưu trữ không còn nhất quán.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
