package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.dto.membership.onboarding.MembershipProfileDraftRequest;
import com.zanh.route_sharing.repository.membership.onboarding.*;
import com.zanh.route_sharing.service.membership.onboarding.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class MembershipDraftCoordinator {
    private final MembershipOnboardingContextRepository contextRepository;
    private final MembershipProfileRepository profileRepository;
    private final MembershipEvidenceRepository evidenceRepository;
    private final MembershipAcademicContextRepository academicRepository;
    private final MembershipEvidenceCommitCoordinator evidenceCoordinator;
    private final MembershipSubmissionRepository submissionRepository;
    private final MembershipOnboardingResponseMapper mapper;
    private final MembershipPolicy policy;

    public MembershipDraftCoordinator(MembershipOnboardingContextRepository contextRepository,
            MembershipProfileRepository profileRepository, MembershipEvidenceRepository evidenceRepository,
            MembershipAcademicContextRepository academicRepository, MembershipEvidenceCommitCoordinator evidenceCoordinator,
            MembershipSubmissionRepository submissionRepository, MembershipOnboardingResponseMapper mapper,
            MembershipPolicy policy) {
        this.contextRepository = contextRepository; this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository; this.academicRepository = academicRepository;
        this.evidenceCoordinator = evidenceCoordinator; this.submissionRepository = submissionRepository;
        this.mapper = mapper; this.policy = policy;
    }

    @Transactional
    public MembershipDraftCommitResult commit(Long actorId, MembershipProfileDraftRequest request,
            List<PreparedMembershipEvidence> prepared) {
        var context = contextRepository.lockContext(actorId);
        policy.requireAccountState(context.account());
        HoSoThanhVien current = profileRepository.lockCurrent(actorId, context.school().getId()).orElse(null);
        policy.requireStudentProfile(current);
        HoSoSinhVien profile = (HoSoSinhVien) current;
        policy.requireDraftEditable(profile, request.expectedVersion());
        if (profile != null) {
            // @Version belongs to the aggregate root. Evidence-only mutations would otherwise
            // leave the parent version unchanged, weakening expectedVersion stale-write protection.
            profileRepository.forceVersionIncrement(profile);
        }
        boolean created = false;
        if (profile == null) {
            profile = HoSoSinhVien.builder()
                    .nguoiDung(context.account()).nhaTruong(context.school())
                    .maDinhDanhNoiBo("MEM-" + UUID.randomUUID())
                    .trangThaiHoSo(TrangThaiHoSoThanhVien.DRAFT)
                    .build();
            profileRepository.persistNew(profile);
            created = true;
        }

        applyClaims(profile, request, context.school().getId());
        List<BangChungThanhVien> currentEvidence = evidenceRepository.findCurrent(profile.getId());
        policy.requireEvidenceShape(evidenceCoordinator.effectiveSlots(currentEvidence, prepared, request.removeEvidenceSlots()));
        List<BangChungThanhVien> effective = evidenceCoordinator.apply(profile, currentEvidence, prepared, request.removeEvidenceSlots());
        profileRepository.flush();
        LanNopHoSoThanhVien submission = submissionRepository.findInitial(profile.getId()).orElse(null);
        return new MembershipDraftCommitResult(mapper.profile(profile, effective, submission), created);
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
