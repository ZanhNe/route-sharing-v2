package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class RideRequestFingerprint {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
    private static final String SCHEMA_VERSION = "ride-request:create:v1";

    public String normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_IDEMPOTENCY_KEY",
                    "Thiếu header Idempotency-Key bắt buộc.");
        }
        String normalized = rawKey.trim();
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key phải dài 8-128 ký tự và chỉ chứa chữ, số, '.', '_', ':', '-'.");
        }
        return normalized;
    }

    public String calculate(
            Long actorUserId,
            Long routeId,
            CreateRideRequestRequest request) {
        if (actorUserId == null || routeId == null || request == null) {
            throw new IllegalArgumentException("Dữ liệu fingerprint không được trống");
        }

        StringBuilder canonical = new StringBuilder(384);
        append(canonical, SCHEMA_VERSION);
        append(canonical, actorUserId.toString());
        append(canonical, routeId.toString());
        append(canonical, request.schoolId() == null ? null : request.schoolId().toString());
        appendPoint(canonical, request.pickup());
        appendPoint(canonical, request.passengerDestination());
        append(canonical, decimal(request.proposedSupportAmount()));
        append(canonical, request.note());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK không hỗ trợ SHA-256", exception);
        }
    }

    private static void appendPoint(StringBuilder target, RouteEndpointRequest point) {
        if (point == null) {
            append(target, null);
            append(target, null);
            append(target, null);
            return;
        }
        append(target, decimal(point.latitude()));
        append(target, decimal(point.longitude()));
        append(target, point.address());
    }

    private static String decimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.signum() == 0) {
            normalized = BigDecimal.ZERO;
        }
        return normalized.toPlainString();
    }

    private static void append(StringBuilder target, String value) {
        if (value == null) {
            target.append("-1:");
            return;
        }
        target.append(value.length()).append(':').append(value);
    }
}
