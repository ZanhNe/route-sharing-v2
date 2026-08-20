package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.VanBanPhapLy;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RegistrationLegalPolicy {

    private final RegistrationEmailPolicy emailPolicy;

    public RegistrationLegalPolicy(RegistrationEmailPolicy emailPolicy) {
        this.emailPolicy = emailPolicy;
    }

    public boolean registrationReady(NhaTruong school, List<VanBanPhapLy> currentDocuments) {
        try {
            validateConfiguration(school, currentDocuments);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public void validateConfiguration(NhaTruong school, List<VanBanPhapLy> currentDocuments) {
        if (school == null || !Boolean.TRUE.equals(school.getDangHoatDong())) {
            throw notFound();
        }
        if (!emailPolicy.hasUsableDomainConfiguration(school.getTenMienEmailChoPhep())) {
            throw unavailable();
        }
        List<VanBanPhapLy> mandatory = mandatory(currentDocuments);
        if (mandatory.isEmpty()) {
            throw unavailable();
        }
        Map<String, Long> currentByCode = new HashMap<>();
        for (VanBanPhapLy document : currentDocuments) {
            if (document.getMaVanBan() == null || document.getMaVanBan().isBlank()
                    || document.getPhienBan() == null || document.getPhienBan().isBlank()
                    || document.getNoiDungUrl() == null || document.getNoiDungUrl().isBlank()) {
                throw invalid();
            }
            Long previous = currentByCode.putIfAbsent(document.getMaVanBan(), document.getId());
            if (previous != null) {
                throw invalid();
            }
        }
    }

    public void validateSubmittedLegalIds(List<Long> submittedIds, List<VanBanPhapLy> currentDocuments) {
        Set<Long> mandatoryIds = new HashSet<>();
        for (VanBanPhapLy document : mandatory(currentDocuments)) {
            mandatoryIds.add(document.getId());
        }
        Set<Long> submitted = new HashSet<>(submittedIds);
        if (submitted.size() != submittedIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Danh sách văn bản pháp lý chấp thuận không được chứa ID trùng lặp.");
        }
        if (!mandatoryIds.containsAll(submitted)) {
            throw new BusinessException(HttpStatus.CONFLICT, "REGISTRATION_LEGAL_DOCUMENTS_CHANGED",
                    "Bộ văn bản pháp lý đăng ký đã thay đổi. Vui lòng tải lại và chấp thuận phiên bản hiện hành.");
        }
        if (!submitted.containsAll(mandatoryIds)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "LEGAL_CONSENT_REQUIRED",
                    "Bạn phải chấp thuận toàn bộ văn bản pháp lý bắt buộc hiện hành.");
        }
    }

    public List<VanBanPhapLy> mandatory(List<VanBanPhapLy> documents) {
        return documents.stream().filter(document -> Boolean.TRUE.equals(document.getBatBuoc())).toList();
    }

    public BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "REGISTRATION_SCHOOL_NOT_FOUND",
                "Không tìm thấy trường đang mở đăng ký.");
    }

    public BusinessException unavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "REGISTRATION_LEGAL_CONFIGURATION_UNAVAILABLE",
                "Trường chưa có cấu hình đăng ký và văn bản pháp lý bắt buộc hợp lệ.");
    }

    public BusinessException invalid() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "REGISTRATION_LEGAL_CONFIGURATION_INVALID",
                "Cấu hình văn bản pháp lý đăng ký hiện không hợp lệ.");
    }
}
