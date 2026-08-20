package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.VanBanPhapLy;
import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationRequest;
import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationLegalContextResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationLegalDocumentResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationSchoolResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.iam.registration.NhaTruongRegistrationRepository;
import com.zanh.route_sharing.repository.iam.registration.VanBanPhapLyRegistrationRepository;
import com.zanh.route_sharing.service.AccountRegistrationService;
import com.zanh.route_sharing.service.iam.registration.AccountRegistrationCommitCoordinator;
import com.zanh.route_sharing.service.iam.registration.AccountRegistrationResponseMapper;
import com.zanh.route_sharing.service.iam.registration.RegistrationAbuseGuard;
import com.zanh.route_sharing.service.iam.registration.RegistrationEmailPolicy;
import com.zanh.route_sharing.service.iam.registration.RegistrationLegalPolicy;
import com.zanh.route_sharing.service.iam.registration.RegistrationPasswordPolicy;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountRegistrationServiceImpl implements AccountRegistrationService {
    private final NhaTruongRegistrationRepository schoolRepository;
    private final VanBanPhapLyRegistrationRepository legalRepository;
    private final RegistrationEmailPolicy emailPolicy;
    private final RegistrationPasswordPolicy passwordPolicy;
    private final RegistrationLegalPolicy legalPolicy;
    private final RegistrationAbuseGuard abuseGuard;
    private final PasswordEncoder passwordEncoder;
    private final AccountRegistrationCommitCoordinator commitCoordinator;
    private final AccountRegistrationResponseMapper responseMapper;
    private final Clock clock;

    public AccountRegistrationServiceImpl(
            NhaTruongRegistrationRepository schoolRepository,
            VanBanPhapLyRegistrationRepository legalRepository,
            RegistrationEmailPolicy emailPolicy,
            RegistrationPasswordPolicy passwordPolicy,
            RegistrationLegalPolicy legalPolicy,
            RegistrationAbuseGuard abuseGuard,
            PasswordEncoder passwordEncoder,
            AccountRegistrationCommitCoordinator commitCoordinator,
            AccountRegistrationResponseMapper responseMapper,
            Clock clock) {
        this.schoolRepository = schoolRepository;
        this.legalRepository = legalRepository;
        this.emailPolicy = emailPolicy;
        this.passwordPolicy = passwordPolicy;
        this.legalPolicy = legalPolicy;
        this.abuseGuard = abuseGuard;
        this.passwordEncoder = passwordEncoder;
        this.commitCoordinator = commitCoordinator;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationSchoolResponse> listRegistrationSchools() {
        Instant now = TimePolicy.now(clock);
        return schoolRepository.findRegistrationCandidates().stream()
                .filter(school -> legalPolicy.registrationReady(
                        school, legalRepository.findCurrentEffectiveForSchool(school.getId(), now)))
                .map(this::toSchoolResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationLegalContextResponse getRegistrationLegalContext(Long schoolId) {
        NhaTruong school = schoolRepository.findActiveForRegistrationRead(schoolId)
                .orElseThrow(legalPolicy::notFound);
        List<VanBanPhapLy> documents = legalRepository.findCurrentEffectiveForSchool(
                schoolId, TimePolicy.now(clock));
        legalPolicy.validateConfiguration(school, documents);
        return new RegistrationLegalContextResponse(
                school.getId(),
                school.getTenTruong(),
                documents.stream().map(this::toLegalDocumentResponse).toList());
    }

    @Override
    public AccountRegistrationResponse register(AccountRegistrationRequest request,
            String remoteAddress,
            String userAgentEvidence) {
        String normalizedName = normalizeName(request.fullName());
        String normalizedEmail = emailPolicy.normalizeEmail(request.schoolEmail());
        validateLegalIdShape(request.acceptedLegalDocumentIds());
        passwordPolicy.validate(request.password());
        abuseGuard.check(remoteAddress, normalizedEmail);
        String encodedPassword = passwordEncoder.encode(request.password());
        Instant now = TimePolicy.now(clock);
        return responseMapper.toResponse(commitCoordinator.register(
                request.schoolId(),
                normalizedName,
                normalizedEmail,
                encodedPassword,
                request.acceptedLegalDocumentIds(),
                remoteAddress,
                userAgentEvidence,
                now).account());
    }

    private String normalizeName(String rawName) {
        String normalized = rawName == null ? "" : rawName.trim();
        if (normalized.isEmpty() || normalized.length() > 255) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_NAME",
                    "Họ tên đăng ký không hợp lệ.");
        }
        return normalized;
    }

    private void validateLegalIdShape(List<Long> ids) {
        Set<Long> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Danh sách văn bản pháp lý chấp thuận không được chứa ID trùng lặp.");
        }
    }

    private RegistrationSchoolResponse toSchoolResponse(NhaTruong school) {
        return new RegistrationSchoolResponse(
                school.getId(),
                school.getMaTruong(),
                school.getTenTruong(),
                school.getTenVietTat(),
                school.getLogoUrl());
    }

    private RegistrationLegalDocumentResponse toLegalDocumentResponse(VanBanPhapLy document) {
        return new RegistrationLegalDocumentResponse(
                document.getId(),
                document.getMaVanBan(),
                document.getLoaiVanBan().name(),
                document.getTenVanBan(),
                document.getPhienBan(),
                document.getNoiDungUrl(),
                Boolean.TRUE.equals(document.getBatBuoc()),
                document.getHieuLucTu());
    }
}
