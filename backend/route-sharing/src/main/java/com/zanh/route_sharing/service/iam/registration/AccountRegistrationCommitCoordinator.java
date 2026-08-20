package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.domain.entity.DongYPhapLy;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.VanBanPhapLy;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.iam.registration.DongYPhapLyRegistrationRepository;
import com.zanh.route_sharing.repository.iam.registration.NhaTruongRegistrationRepository;
import com.zanh.route_sharing.repository.iam.registration.VanBanPhapLyRegistrationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class AccountRegistrationCommitCoordinator {
    private final NguoiDungRepository accountRepository;
    private final NhaTruongRegistrationRepository schoolRepository;
    private final VanBanPhapLyRegistrationRepository legalRepository;
    private final DongYPhapLyRegistrationRepository consentRepository;
    private final RegistrationEmailPolicy emailPolicy;
    private final RegistrationLegalPolicy legalPolicy;

    public AccountRegistrationCommitCoordinator(
            NguoiDungRepository accountRepository,
            NhaTruongRegistrationRepository schoolRepository,
            VanBanPhapLyRegistrationRepository legalRepository,
            DongYPhapLyRegistrationRepository consentRepository,
            RegistrationEmailPolicy emailPolicy,
            RegistrationLegalPolicy legalPolicy) {
        this.accountRepository = accountRepository;
        this.schoolRepository = schoolRepository;
        this.legalRepository = legalRepository;
        this.consentRepository = consentRepository;
        this.emailPolicy = emailPolicy;
        this.legalPolicy = legalPolicy;
    }

    @Transactional
    public RegistrationCommitResult register(Long schoolId,
                                             String normalizedName,
                                             String normalizedEmail,
                                             String encodedPassword,
                                             List<Long> acceptedLegalDocumentIds,
                                             String remoteAddress,
                                             String userAgentEvidence,
                                             Instant now) {
        NhaTruong school = schoolRepository.findActiveForRegistrationLock(schoolId)
                .orElseThrow(legalPolicy::notFound);
        List<VanBanPhapLy> currentDocuments = legalRepository.findCurrentEffectiveForSchool(schoolId, now);
        legalPolicy.validateConfiguration(school, currentDocuments);
        emailPolicy.requireAllowedDomain(normalizedEmail, school.getTenMienEmailChoPhep());
        legalPolicy.validateSubmittedLegalIds(acceptedLegalDocumentIds, currentDocuments);

        if (accountRepository.findByEmailTruongIgnoreCase(normalizedEmail).isPresent()) {
            throw duplicateEmail();
        }

        try {
            NguoiDung account = NguoiDung.builder()
                    .hoTen(normalizedName)
                    .emailTruong(normalizedEmail)
                    .matKhauDaMaHoa(encodedPassword)
                    .trangThaiTaiKhoan(TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL)
                    .emailDaXacThucLuc(null)
                    .build();
            NguoiDung persistedAccount = accountRepository.saveAndFlush(account);

            List<DongYPhapLy> consents = legalPolicy.mandatory(currentDocuments).stream()
                    .map(document -> buildConsent(
                            persistedAccount, document, now, remoteAddress, userAgentEvidence))
                    .toList();
            consentRepository.saveAll(consents);
            consentRepository.flush();
            return new RegistrationCommitResult(persistedAccount);
        } catch (DataIntegrityViolationException ex) {
            if (isEmailConstraint(ex)) {
                throw duplicateEmail();
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_INTEGRITY_VIOLATION",
                    "Không thể hoàn tất đăng ký do xung đột ràng buộc dữ liệu.");
        }
    }


    private static DongYPhapLy buildConsent(NguoiDung account,
                                             VanBanPhapLy document,
                                             Instant now,
                                             String remoteAddress,
                                             String userAgentEvidence) {
        return DongYPhapLy.builder()
                .nguoiDung(account)
                .vanBanPhapLy(document)
                .dongYLuc(now)
                .diaChiIp(remoteAddress)
                .thongTinTrinhDuyet(userAgentEvidence)
                .build();
    }

    private static boolean isEmailConstraint(DataIntegrityViolationException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && (message.contains("uk_nguoi_dung_email_truong_lower")
                    || message.contains("uk_nguoi_dung_email_truong"))) {
                return true;
            }
            if (cursor instanceof ConstraintViolationException constraint) {
                String name = constraint.getConstraintName();
                if ("uk_nguoi_dung_email_truong_lower".equalsIgnoreCase(name)
                        || "uk_nguoi_dung_email_truong".equalsIgnoreCase(name)) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static BusinessException duplicateEmail() {
        return new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "Email trường này đã được đăng ký.");
    }
}
