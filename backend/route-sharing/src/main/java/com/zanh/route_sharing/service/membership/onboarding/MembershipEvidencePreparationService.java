package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.enums.ViTriBangChungThanhVien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.evidence.*;
import com.zanh.route_sharing.service.membership.onboarding.model.PreparedMembershipEvidence;
import com.zanh.route_sharing.storage.evidence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Component
public class MembershipEvidencePreparationService {
    private final EvidenceBinaryStorage storage;
    private final EvidenceContentInspector inspector;
    private final EvidenceUploadLimitPolicy uploadLimit;
    private final EvidenceFilenamePolicy filenamePolicy;
    private final MembershipPolicy membershipPolicy;

    public MembershipEvidencePreparationService(EvidenceBinaryStorage storage, EvidenceContentInspector inspector,
            EvidenceUploadLimitPolicy uploadLimit, EvidenceFilenamePolicy filenamePolicy, MembershipPolicy membershipPolicy) {
        this.storage = storage; this.inspector = inspector; this.uploadLimit = uploadLimit;
        this.filenamePolicy = filenamePolicy; this.membershipPolicy = membershipPolicy;
    }

    public List<PreparedMembershipEvidence> prepare(MultipartFile front, MultipartFile back, MultipartFile confirmation) {
        List<PreparedMembershipEvidence> result = new ArrayList<>();
        try {
            add(result, ViTriBangChungThanhVien.STUDENT_CARD_FRONT, front);
            add(result, ViTriBangChungThanhVien.STUDENT_CARD_BACK, back);
            add(result, ViTriBangChungThanhVien.OFFICIAL_STUDENT_CONFIRMATION, confirmation);
            return List.copyOf(result);
        } catch (RuntimeException ex) {
            cleanup(result);
            throw ex;
        }
    }

    public void cleanup(Collection<PreparedMembershipEvidence> prepared) {
        if (prepared == null) return;
        prepared.forEach(p -> storage.cleanupStage(p.staged()));
    }

    private void add(List<PreparedMembershipEvidence> result, ViTriBangChungThanhVien slot, MultipartFile file) {
        if (file == null) return;
        if (file.isEmpty()) throw invalidContent();
        uploadLimit.requireWithinLimit(file.getSize());
        StagedBinary staged = null;
        try {
            staged = storage.stage(file.getInputStream());
            uploadLimit.requireWithinLimit(staged.sizeBytes());
            EvidenceInspection inspection = inspector.inspect(staged.path());
            membershipPolicy.requireEvidenceMediaAllowed(inspection.verifiedMediaType());
            result.add(new PreparedMembershipEvidence(slot, staged,
                    filenamePolicy.normalize(file.getOriginalFilename()), inspection.verifiedMediaType()));
        } catch (BusinessException ex) {
            storage.cleanupStage(staged);
            throw ex;
        } catch (IOException ex) {
            storage.cleanupStage(staged);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EVIDENCE_STORAGE_UNAVAILABLE",
                    "Kho bằng chứng hiện không khả dụng.");
        }
    }

    private static BusinessException invalidContent() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "EVIDENCE_CONTENT_INVALID",
                "Tệp bằng chứng không được rỗng.");
    }
}
