package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.config.properties.EmailVerificationProperties;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhienXacThucTaiKhoan;
import com.zanh.route_sharing.domain.enums.MucDichXacThucTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiPhienXacThucTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.iam.emailverification.NguoiDungEmailVerificationRepository;
import com.zanh.route_sharing.repository.iam.emailverification.PhienXacThucTaiKhoanRepository;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class EmailVerificationCommitCoordinator {
    private static final MucDichXacThucTaiKhoan PURPOSE = MucDichXacThucTaiKhoan.DANG_KY_EMAIL;
    private static final String TOMBSTONE_PREFIX = "TOMBSTONED:";
    private static final EnumSet<TrangThaiPhienXacThucTaiKhoan> TERMINAL_STATES = EnumSet.of(
            TrangThaiPhienXacThucTaiKhoan.SUCCESS,
            TrangThaiPhienXacThucTaiKhoan.FAILED,
            TrangThaiPhienXacThucTaiKhoan.EXPIRED,
            TrangThaiPhienXacThucTaiKhoan.LOCKED,
            TrangThaiPhienXacThucTaiKhoan.CANCELLED);

    private final NguoiDungEmailVerificationRepository accountRepository;
    private final PhienXacThucTaiKhoanRepository challengeRepository;
    private final EmailVerificationCodeProtector protector;
    private final EmailVerificationProperties properties;
    private final Clock clock;
    private final EntityManager entityManager;

    public EmailVerificationCommitCoordinator(
            NguoiDungEmailVerificationRepository accountRepository,
            PhienXacThucTaiKhoanRepository challengeRepository,
            EmailVerificationCodeProtector protector,
            EmailVerificationProperties properties,
            Clock clock,
            EntityManager entityManager) {
        this.accountRepository = accountRepository;
        this.challengeRepository = challengeRepository;
        this.protector = protector;
        this.properties = properties;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public PreparedEmailVerification prepareCandidate(Long accountId, String plaintextCode) {
        Instant now = TimePolicy.now(clock);
        cleanupTerminalMetadata(now);
        NguoiDung account = requireAccountForUpdate(accountId);
        requirePendingEmail(account);
        normalizeStaleCreated(accountId, now);
        enforceRequestCooldown(accountId, now);

        String protectedCode = protector.protect(account.getId(), account.getEmailTruong(), plaintextCode);
        PhienXacThucTaiKhoan challenge = PhienXacThucTaiKhoan.builder()
                .mucDich(PURPOSE)
                .emailNhan(account.getEmailTruong())
                .maOtpDaBam(protectedCode)
                .hetHanLuc(now.plus(properties.getTtl()))
                .soLanThu(0)
                .soLanThuToiDa(properties.getMaxWrongAttempts())
                .trangThai(TrangThaiPhienXacThucTaiKhoan.CREATED)
                .nguoiDung(account)
                .build();
        challengeRepository.saveAndFlush(challenge);
        return new PreparedEmailVerification(account.getId(), challenge.getId(), account.getEmailTruong(), plaintextCode);
    }

    @Transactional
    public void markDelivered(Long accountId, Long challengeId) {
        Instant now = TimePolicy.now(clock);
        NguoiDung account = requireAccountForUpdate(accountId);
        PhienXacThucTaiKhoan candidate = challengeRepository.findByIdAndNguoiDungId(challengeId, accountId)
                .orElse(null);
        if (candidate == null || candidate.getTrangThai() != TrangThaiPhienXacThucTaiKhoan.CREATED
                || candidate.getMucDich() != PURPOSE) {
            return;
        }
        if (!pendingEmail(account) || !sameEmail(account.getEmailTruong(), candidate.getEmailNhan())) {
            tombstone(candidate, TrangThaiPhienXacThucTaiKhoan.CANCELLED, "INELIGIBLE_AFTER_DELIVERY");
            return;
        }

        List<PhienXacThucTaiKhoan> priorUsable = challengeRepository
                .findByNguoiDungIdAndMucDichAndTrangThaiIn(
                        accountId,
                        PURPOSE,
                        EnumSet.of(TrangThaiPhienXacThucTaiKhoan.SENT));
        for (PhienXacThucTaiKhoan prior : priorUsable) {
            if (!prior.getId().equals(candidate.getId())) {
                tombstone(prior, TrangThaiPhienXacThucTaiKhoan.CANCELLED, "REPLACED");
            }
        }
        candidate.setTrangThai(TrangThaiPhienXacThucTaiKhoan.SENT);
        candidate.setGuiLuc(now);
        candidate.setHetHanLuc(now.plus(properties.getTtl()));
        challengeRepository.save(candidate);
    }

    @Transactional
    public void markDeliveryFailed(Long accountId, Long challengeId) {
        requireAccountForUpdate(accountId);
        challengeRepository.findByIdAndNguoiDungId(challengeId, accountId)
                .filter(candidate -> candidate.getMucDich() == PURPOSE)
                .filter(candidate -> candidate.getTrangThai() == TrangThaiPhienXacThucTaiKhoan.CREATED)
                .ifPresent(candidate -> tombstone(candidate, TrangThaiPhienXacThucTaiKhoan.FAILED, "DELIVERY_FAILED"));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public VerifiedEmailAccount verify(Long accountId, String candidateCode) {
        Instant now = TimePolicy.now(clock);
        cleanupTerminalMetadata(now);
        NguoiDung account = requireAccountForUpdate(accountId);
        requirePendingEmail(account);

        PhienXacThucTaiKhoan challenge = challengeRepository
                .findFirstByNguoiDungIdAndMucDichAndTrangThaiOrderByCreatedAtDesc(
                        accountId, PURPOSE, TrangThaiPhienXacThucTaiKhoan.SENT)
                .orElseThrow(EmailVerificationCommitCoordinator::invalidCode);

        if (!sameEmail(account.getEmailTruong(), challenge.getEmailNhan())) {
            tombstone(challenge, TrangThaiPhienXacThucTaiKhoan.CANCELLED, "EMAIL_CHANGED");
            throw invalidCode();
        }

        if (challenge.getSoLanThu() >= challenge.getSoLanThuToiDa()) {
            tombstone(challenge, TrangThaiPhienXacThucTaiKhoan.LOCKED, "ATTEMPT_LIMIT");
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "EMAIL_VERIFICATION_CODE_LOCKED",
                    "Mã xác thực email không còn khả dụng do vượt quá số lần thử cho phép.");
        }

        boolean matches = protector.matches(
                challenge.getMaOtpDaBam(), accountId, account.getEmailTruong(), candidateCode);

        if (!now.isBefore(challenge.getHetHanLuc())) {
            tombstone(challenge, TrangThaiPhienXacThucTaiKhoan.EXPIRED, "EXPIRED");
            if (matches) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "EMAIL_VERIFICATION_CODE_EXPIRED",
                        "Mã xác thực email đã hết hạn.");
            }
            throw invalidCode();
        }

        int nextAttempts = challenge.getSoLanThu() + (matches ? 0 : 1);
        if (!matches) {
            challenge.setSoLanThu(nextAttempts);
            if (nextAttempts >= challenge.getSoLanThuToiDa()) {
                tombstone(challenge, TrangThaiPhienXacThucTaiKhoan.LOCKED, "ATTEMPT_LIMIT");
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "EMAIL_VERIFICATION_CODE_LOCKED",
                        "Mã xác thực email không còn khả dụng do vượt quá số lần thử cho phép.");
            }
            challengeRepository.save(challenge);
            throw invalidCode();
        }

        challenge.setTrangThai(TrangThaiPhienXacThucTaiKhoan.SUCCESS);
        challenge.setHoanThanhLuc(now);
        challenge.setMaOtpDaBam(TOMBSTONE_PREFIX + "SUCCESS");
        challengeRepository.save(challenge);

        cancelOtherNonTerminalChallenges(accountId, challenge.getId());
        account.setEmailDaXacThucLuc(now);
        account.setTrangThaiTaiKhoan(TrangThaiTaiKhoan.CHO_DUYET_HO_SO);
        accountRepository.saveAndFlush(account);

        // PostgreSQL security trigger bumps security_version on account-state change.
        // Refresh after flush so the replacement ONBOARDING token binds to that committed version.
        entityManager.refresh(account);
        return new VerifiedEmailAccount(
                account.getId(),
                account.getEmailTruong(),
                account.getTrangThaiTaiKhoan(),
                account.getSecurityVersion() == null ? 0L : account.getSecurityVersion(),
                account.getEmailDaXacThucLuc());
    }

    private void cleanupTerminalMetadata(Instant now) {
        challengeRepository.deleteTerminalBefore(
                PURPOSE,
                TERMINAL_STATES,
                now.minus(properties.getTerminalRowRetention()));
    }

    private void normalizeStaleCreated(Long accountId, Instant now) {
        List<PhienXacThucTaiKhoan> created = challengeRepository.findByNguoiDungIdAndMucDichAndTrangThaiIn(
                accountId, PURPOSE, EnumSet.of(TrangThaiPhienXacThucTaiKhoan.CREATED));
        for (PhienXacThucTaiKhoan challenge : created) {
            Instant createdAt = challenge.getCreatedAt();
            if (createdAt != null && !now.isBefore(createdAt.plus(properties.getResendCooldown()))) {
                tombstone(challenge, TrangThaiPhienXacThucTaiKhoan.FAILED, "DELIVERY_WINDOW_ELAPSED");
            }
        }
    }

    private void enforceRequestCooldown(Long accountId, Instant now) {
        challengeRepository.findFirstByNguoiDungIdAndMucDichOrderByCreatedAtDesc(accountId, PURPOSE)
                .ifPresent(latest -> {
                    Instant requestedAt = latest.getCreatedAt();
                    if (requestedAt == null || now.isBefore(requestedAt.plus(properties.getResendCooldown()))) {
                        throw rateLimited();
                    }
                });
    }

    private void cancelOtherNonTerminalChallenges(Long accountId, Long successfulChallengeId) {
        List<PhienXacThucTaiKhoan> candidates = challengeRepository.findByNguoiDungIdAndMucDichAndTrangThaiIn(
                accountId,
                PURPOSE,
                EnumSet.of(TrangThaiPhienXacThucTaiKhoan.CREATED, TrangThaiPhienXacThucTaiKhoan.SENT));
        for (PhienXacThucTaiKhoan candidate : candidates) {
            if (!candidate.getId().equals(successfulChallengeId)) {
                tombstone(candidate, TrangThaiPhienXacThucTaiKhoan.CANCELLED, "ACCOUNT_VERIFIED");
            }
        }
    }

    private NguoiDung requireAccountForUpdate(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Yêu cầu cần phiên onboarding hợp lệ.");
        }
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "TOKEN_STALE",
                        "Phiên onboarding không còn hợp lệ."));
    }

    private static void requirePendingEmail(NguoiDung account) {
        if (!pendingEmail(account)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "TOKEN_STALE",
                    "Phiên onboarding không còn phù hợp với trạng thái tài khoản hiện tại.");
        }
    }

    private static boolean pendingEmail(NguoiDung account) {
        return account.getTrangThaiTaiKhoan() == TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL
                && account.getEmailDaXacThucLuc() == null;
    }

    private static boolean sameEmail(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private void tombstone(PhienXacThucTaiKhoan challenge,
            TrangThaiPhienXacThucTaiKhoan terminalStatus,
            String reason) {
        challenge.setTrangThai(terminalStatus);
        challenge.setMaOtpDaBam(TOMBSTONE_PREFIX + reason);
        challengeRepository.save(challenge);
    }

    private static BusinessException invalidCode() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "EMAIL_VERIFICATION_CODE_INVALID",
                "Mã xác thực email không hợp lệ.");
    }

    private static BusinessException rateLimited() {
        return new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "EMAIL_VERIFICATION_RATE_LIMITED",
                "Yêu cầu mã xác thực email đang được giới hạn. Vui lòng thử lại sau.");
    }
}
