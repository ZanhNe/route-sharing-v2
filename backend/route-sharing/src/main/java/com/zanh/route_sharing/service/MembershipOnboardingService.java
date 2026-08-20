package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.membership.onboarding.*;
import com.zanh.route_sharing.service.membership.onboarding.model.*;
import org.springframework.web.multipart.MultipartFile;

public interface MembershipOnboardingService {
        MembershipProfileResponse getCurrent(Long actorId);

        MembershipDraftCommitResult saveDraft(Long actorId, MembershipProfileDraftRequest request,
                        MultipartFile studentCardFront, MultipartFile studentCardBack,
                        MultipartFile officialStudentConfirmation);

        MembershipSubmissionResponse submit(Long actorId, MembershipProfileDraftRequest request,
                        MultipartFile studentCardFront, MultipartFile studentCardBack,
                        MultipartFile officialStudentConfirmation);

        MembershipEvidenceDownloadResult downloadEvidence(Long actorId, Long evidenceId);
}
