package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryRegistrationAbuseGuard implements RegistrationAbuseGuard {
    private final RegistrationAbuseProperties properties;
    private final Clock clock;
    private final LinkedHashMap<String, Bucket> buckets = new LinkedHashMap<>();

    public InMemoryRegistrationAbuseGuard(RegistrationAbuseProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public synchronized void check(String remoteAddress, String normalizedEmail) {
        Instant now = clock.instant();
        purgeExpired(now);
        String ip = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
        requireWithin("ip:" + ip, properties.getPerIpAttempts(), now);
        requireWithin("email:" + sha256(normalizedEmail == null ? "" : normalizedEmail),
                properties.getPerEmailAttempts(), now);
        evictOverflow();
    }

    private void requireWithin(String key, int limit, Instant now) {
        Bucket bucket = buckets.get(key);
        if (bucket == null || expired(bucket, now)) {
            buckets.put(key, new Bucket(now, 1));
            return;
        }
        if (bucket.attempts >= limit) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "REGISTRATION_RATE_LIMITED",
                    "Có quá nhiều yêu cầu đăng ký. Vui lòng thử lại sau.");
        }
        buckets.put(key, new Bucket(bucket.windowStart, bucket.attempts + 1));
    }

    private void purgeExpired(Instant now) {
        Iterator<Map.Entry<String, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            if (expired(iterator.next().getValue(), now)) {
                iterator.remove();
            }
        }
    }

    private boolean expired(Bucket bucket, Instant now) {
        return !now.isBefore(bucket.windowStart.plus(properties.getWindow()));
    }

    private void evictOverflow() {
        while (buckets.size() > properties.getMaxTrackedKeys()) {
            Iterator<String> it = buckets.keySet().iterator();
            if (!it.hasNext()) {
                return;
            }
            it.next();
            it.remove();
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private record Bucket(Instant windowStart, int attempts) {}
}
