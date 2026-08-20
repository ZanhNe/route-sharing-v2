package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.config.properties.EmailVerificationProtectionProperties;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Component
public class HmacEmailVerificationCodeProtector implements EmailVerificationCodeProtector {
    private static final String ALGORITHM = "HmacSHA256";
    private static final String FORMAT_PREFIX = "h1";
    private static final int SALT_BYTES = 16;

    private final EmailVerificationProtectionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public HmacEmailVerificationCodeProtector(EmailVerificationProtectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String protect(Long accountId, String email, String code) {
        String version = requireActiveVersion();
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] mac = hmac(requireKey(version), payload(accountId, email, salt, code));
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return FORMAT_PREFIX + ":" + version + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(mac);
    }

    @Override
    public boolean matches(String protectedValue, Long accountId, String email, String candidateCode) {
        if (protectedValue == null || protectedValue.startsWith("TOMBSTONED:")) {
            return false;
        }
        String[] parts = protectedValue.split(":", -1);
        if (parts.length != 4 || !FORMAT_PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            String version = parts[1];
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            byte[] actual = hmac(requireKey(version), payload(accountId, email, salt, candidateCode));
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String requireActiveVersion() {
        String version = properties.getActiveKeyVersion();
        if (version == null || version.isBlank()) {
            throw protectionUnavailable();
        }
        requireKey(version);
        return version.trim();
    }

    private byte[] requireKey(String version) {
        String configured = properties.getKeys().get(version);
        if (configured == null || configured.isBlank()) {
            throw protectionUnavailable();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configured.trim());
            if (decoded.length < 32) {
                throw protectionUnavailable();
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw protectionUnavailable();
        }
    }

    private static byte[] payload(Long accountId, String email, byte[] salt, String code) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String prefix = "DANG_KY_EMAIL|" + accountId + "|" + normalizedEmail + "|";
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] codeBytes = ("|" + code).getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[prefixBytes.length + salt.length + codeBytes.length];
        System.arraycopy(prefixBytes, 0, payload, 0, prefixBytes.length);
        System.arraycopy(salt, 0, payload, prefixBytes.length, salt.length);
        System.arraycopy(codeBytes, 0, payload, prefixBytes.length + salt.length, codeBytes.length);
        return payload;
    }

    private static byte[] hmac(byte[] key, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể khởi tạo HMAC xác thực email.", exception);
        }
    }

    private static BusinessException protectionUnavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "EMAIL_VERIFICATION_PROTECTION_UNAVAILABLE",
                "Dịch vụ bảo vệ mã xác thực email hiện không khả dụng.");
    }
}
