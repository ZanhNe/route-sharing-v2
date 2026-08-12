package com.zanh.route_sharing.security.boarding;

import com.zanh.route_sharing.config.properties.BoardingCodeProperties;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.security.boarding.model.BoardingCodeBinding;
import com.zanh.route_sharing.security.boarding.model.ProtectedBoardingCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Component
public class AesGcmBoardingCodeProtector implements BoardingCodeProtector {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final BoardingCodeProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public AesGcmBoardingCodeProtector(BoardingCodeProperties properties) {
        this(properties, new SecureRandom());
    }

    AesGcmBoardingCodeProtector(BoardingCodeProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    @Override
    public ProtectedBoardingCode protect(String boardingCode, BoardingCodeBinding binding) {
        requireCode(boardingCode);
        String version = activeVersion();
        SecretKey key = keyFor(version);
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(binding));
            byte[] encrypted = cipher.doFinal(boardingCode.getBytes(StandardCharsets.US_ASCII));
            return new ProtectedBoardingCode(encrypted, nonce, version);
        } catch (GeneralSecurityException exception) {
            throw protectionUnavailable();
        }
    }

    @Override
    public String reveal(ProtectedBoardingCode protectedCode, BoardingCodeBinding binding) {
        if (protectedCode == null) {
            throw invariantViolation();
        }
        SecretKey key = keyFor(protectedCode.keyVersion());
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, protectedCode.nonce()));
            cipher.updateAAD(aad(binding));
            String code = new String(cipher.doFinal(protectedCode.encryptedCode()), StandardCharsets.US_ASCII);
            requireCode(code);
            return code;
        } catch (AEADBadTagException exception) {
            throw invariantViolation();
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw invariantViolation();
        }
    }

    private String activeVersion() {
        String version = properties == null ? null : properties.getActiveKeyVersion();
        if (version == null || version.isBlank() || version.trim().length() > 64) {
            throw protectionUnavailable();
        }
        return version.trim();
    }

    private SecretKey keyFor(String version) {
        if (version == null || version.isBlank()) {
            throw protectionUnavailable();
        }
        Map<String, String> keys = properties == null ? null : properties.getKeys();
        String encoded = keys == null ? null : keys.get(version);
        if (encoded == null || encoded.isBlank()) {
            throw protectionUnavailable();
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encoded.trim());
            if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
                throw new IllegalArgumentException("invalid AES key length");
            }
            return new SecretKeySpec(raw, "AES");
        } catch (IllegalArgumentException exception) {
            throw protectionUnavailable();
        }
    }

    private static byte[] aad(BoardingCodeBinding binding) {
        if (binding == null) {
            throw invariantViolation();
        }
        String canonical = "trip=" + binding.tripId()
                + "|request=" + binding.rideRequestId()
                + "|pickup=" + binding.pickupStopId();
        return canonical.getBytes(StandardCharsets.US_ASCII);
    }

    private static void requireCode(String code) {
        if (code == null || !code.matches("[0-9]{6}")) {
            throw invariantViolation();
        }
    }

    private static BusinessException protectionUnavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "BOARDING_CREDENTIAL_PROTECTION_UNAVAILABLE",
                "Hệ thống bảo vệ boarding credential tạm thời không khả dụng.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "BOARDING_CREDENTIAL_INVARIANT_VIOLATION",
                "Boarding credential đang lưu không nhất quán.");
    }
}
