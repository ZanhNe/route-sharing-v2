package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.dto.membership.onboarding.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.membership.onboarding.*;
import com.zanh.route_sharing.service.membership.onboarding.model.PreparedMembershipEvidence;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Component
public class MembershipSubmissionCoordinator {
    private final MembershipOnboardingContextRepository contextRepository;
    private final MembershipProfileRepository profileRepository;
    private final MembershipEvidenceRepository evidenceRepository;
    private final MembershipSubmissionRepository submissionRepository;
    private final MembershipAcademicContextRepository academicRepository;
    private final MembershipEvidenceCommitCoordinator evidenceCoordinator;
    private final MembershipMaterialComparator materialComparator;
    private final MembershipOnboardingResponseMapper mapper;
    private final MembershipPolicy policy;
    private final Clock clock;

    public MembershipSubmissionCoordinator(MembershipOnboardingContextRepository contextRepository,
            MembershipProfileRepository profileRepository, MembershipEvidenceRepository evidenceRepository,
            MembershipSubmissionRepository submissionRepository, MembershipAcademicContextRepository academicRepository,
            MembershipEvidenceCommitCoordinator evidenceCoordinator, MembershipMaterialComparator materialComparator,
            MembershipOnboardingResponseMapper mapper, MembershipPolicy policy, Clock clock) {
        this.contextRepository = contextRepository; this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository; this.submissionRepository = submissionRepository;
        this.academicRepository = academicRepository; this.evidenceCoordinator = evidenceCoordinator;
        this.materialComparator = materialComparator; this.mapper = mapper; this.policy = policy; this.clock = clock;
    }

    @Transactional
    public MembershipSubmissionResponse commit(Long actorId, MembershipProfileDraftRequest request,
            List<PreparedMembershipEvidence> prepared) {
        var context = contextRepository.lockContext(actorId);
        policy.requireAccountState(context.account());
        NhaTruong school = contextRepository.lockSchool(context.school().getId());
        HoSoThanhVien current = profileRepository.lockCurrent(actorId, school.getId()).orElse(null);
        policy.requireStudentProfile(current);
        HoSoSinhVien profile = (HoSoSinhVien) current;

        Set<ViTriBangChungThanhVien> supplied = EnumSet.noneOf(ViTriBangChungThanhVien.class);
        prepared.forEach(p -> supplied.add(p.slot()));
        policy.validateSlotMutation(request, supplied);

        if (profile != null && profile.getTrangThaiHoSo() == TrangThaiHoSoThanhVien.SUBMITTED) {
            // Retry-by-state must reconcile to the already committed business truth. A later
            // school deactivation cannot retroactively turn an exact retry into a new failure.
            return retrySubmitted(profile, request, prepared, school);
        }
        policy.requireSchoolActive(school);
        policy.requireDraftEditable(profile, request.expectedVersion());
        if (profile == null) {
            profile = HoSoSinhVien.builder()
                    .nguoiDung(context.account()).nhaTruong(school)
                    .maDinhDanhNoiBo("MEM-" + UUID.randomUUID())
                    .trangThaiHoSo(TrangThaiHoSoThanhVien.DRAFT)
                    .build();
            profileRepository.persistNew(profile);
        }

        applyClaims(profile, request, school.getId());
        List<BangChungThanhVien> currentEvidence = evidenceRepository.findCurrent(profile.getId());
        Set<ViTriBangChungThanhVien> effectiveSlots = evidenceCoordinator.effectiveSlots(
                currentEvidence, prepared, request.removeEvidenceSlots());
        policy.requireSubmitMaterial(profile, effectiveSlots);
        if (profileRepository.studentCodeReservedByOtherIdentity(school.getId(), profile.getMaSoSinhVien(), profile.getId())) {
            throw policy.identifierUnavailable();
        }
        List<BangChungThanhVien> effective = evidenceCoordinator.apply(
                profile, currentEvidence, prepared, request.removeEvidenceSlots());

        LanNopHoSoThanhVien submission = LanNopHoSoThanhVien.builder()
                .hoSoThanhVien(profile)
                .lanNop(1)
                .nopLuc(TimePolicy.now(clock))
                .policyKey(MembershipPolicy.POLICY_KEY)
                .policyVersion(MembershipPolicy.POLICY_VERSION)
                .studentCodeSnapshot(profile.getMaSoSinhVien())
                .currentlyStudyingSnapshot(true)
                .ngayNhapHocSnapshot(profile.getNgayNhapHoc())
                .lopIdSnapshot(profile.getLop() == null ? null : profile.getLop().getId())
                .schoolIdSnapshot(school.getId())
                .bangChungDaNop(new LinkedHashSet<>(effective))
                .build();
        profile.setTrangThaiHoSo(TrangThaiHoSoThanhVien.SUBMITTED);
        submissionRepository.persist(submission);
        profileRepository.flush();
        return mapper.submission(profile, effective, submission, true);
    }

    private MembershipSubmissionResponse retrySubmitted(HoSoSinhVien profile, MembershipProfileDraftRequest request,
            List<PreparedMembershipEvidence> prepared, NhaTruong school) {
        LanNopHoSoThanhVien submission = submissionRepository.findInitial(profile.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "MEMBERSHIP_CONTEXT_INVARIANT_VIOLATION", "Thiếu snapshot lần nộp ban đầu."));
        List<BangChungThanhVien> currentEvidence = evidenceRepository.findCurrent(profile.getId());
        String code = request.studentCode() == null ? profile.getMaSoSinhVien() : policy.normalizeStudentCode(request.studentCode());
        TrangThaiHocTap study = policy.toStudyState(request.currentlyStudying(), profile.getTrangThaiHocTap());
        var enrollment = request.enrollmentDate() == null ? profile.getNgayNhapHoc() : request.enrollmentDate();
        Long classId = profile.getLop() == null ? null : profile.getLop().getId();
        if (request.classId() != null) {
            Lop clazz = academicRepository.findClassInSchool(request.classId(), school.getId()).orElseThrow(policy::classSchoolMismatch);
            classId = clazz.getId();
        }
        if (!materialComparator.matches(submission, code, study, enrollment, classId, school.getId(),
                currentEvidence, prepared, request.removeEvidenceSlots())) {
            throw new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_PROFILE_ALREADY_SUBMITTED",
                    "Hồ sơ ban đầu đã được nộp với nội dung khác.");
        }
        return mapper.submission(profile, currentEvidence, submission, false);
    }

    private void applyClaims(HoSoSinhVien profile, MembershipProfileDraftRequest request, Long schoolId) {
        if (request.studentCode() != null) profile.setMaSoSinhVien(policy.normalizeStudentCode(request.studentCode()));
        if (request.currentlyStudying() != null) {
            profile.setTrangThaiHocTap(policy.toStudyState(request.currentlyStudying(), profile.getTrangThaiHocTap()));
        }
        if (request.enrollmentDate() != null) profile.setNgayNhapHoc(request.enrollmentDate());
        if (request.classId() != null) {
            Lop clazz = academicRepository.findClassInSchool(request.classId(), schoolId).orElseThrow(policy::classSchoolMismatch);
            profile.setLop(clazz);
        }
    }
}
