package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class StateAwareCredentialAuthenticator {
    private final NguoiDungSecurityRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public StateAwareCredentialAuthenticator(NguoiDungSecurityRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public VerifiedAccountCredential authenticate(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        NguoiDung account = repository.findByEmailTruongIgnoreCase(normalizedEmail).orElse(null);
        if (account == null) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, dummyPasswordHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(rawPassword, account.getMatKhauDaMaHoa())) {
            throw invalidCredentials();
        }

        requireCredentialEntryAllowed(account.getTrangThaiTaiKhoan());
        return new VerifiedAccountCredential(
                account.getId(),
                account.getHoTen(),
                account.getEmailTruong(),
                account.getTrangThaiTaiKhoan(),
                account.getEmailDaXacThucLuc(),
                account.getSecurityVersion() == null ? 0L : account.getSecurityVersion());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw invalidCredentials();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireCredentialEntryAllowed(TrangThaiTaiKhoan status) {
        if (status == TrangThaiTaiKhoan.SUSPENDED) {
            throw blocked("ACCOUNT_SUSPENDED", "Tài khoản đang bị đình chỉ.");
        }
        if (status == TrangThaiTaiKhoan.BANNED) {
            throw blocked("ACCOUNT_BANNED", "Tài khoản đã bị cấm.");
        }
        if (status == TrangThaiTaiKhoan.DEACTIVATED) {
            throw blocked("ACCOUNT_DEACTIVATED", "Tài khoản đã ngừng hoạt động.");
        }
        if (status == null) {
            throw blocked("ACCOUNT_STATE_INVALID", "Trạng thái tài khoản không hợp lệ.");
        }
    }

    private static BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, code, message);
    }
}
