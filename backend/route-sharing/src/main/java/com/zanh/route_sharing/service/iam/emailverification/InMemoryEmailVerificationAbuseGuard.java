package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.config.properties.EmailVerificationAbuseProperties;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryEmailVerificationAbuseGuard implements EmailVerificationAbuseGuard {
    private final EmailVerificationAbuseProperties properties;
    private final Clock clock;
    private final LinkedHashMap<String, Bucket> buckets = new LinkedHashMap<>();

    public InMemoryEmailVerificationAbuseGuard(EmailVerificationAbuseProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public synchronized void checkRequest(Long accountId, String remoteAddress) {
        Instant now = clock.instant();
        purgeExpired(now);
        requireWithin("request:account:" + accountKey(accountId),
                properties.getRequestPerAccount(), properties.getRequestWindow(), now);
        requireWithin("request:ip:" + ipKey(remoteAddress),
                properties.getRequestPerIp(), properties.getRequestWindow(), now);
        evictOverflow();
    }

    @Override
    public synchronized void checkVerify(Long accountId, String remoteAddress) {
        Instant now = clock.instant();
        purgeExpired(now);
        requireWithin("verify:account:" + accountKey(accountId),
                properties.getVerifyPerAccount(), properties.getVerifyWindow(), now);
        requireWithin("verify:ip:" + ipKey(remoteAddress),
                properties.getVerifyPerIp(), properties.getVerifyWindow(), now);
        evictOverflow();
    }

    private void requireWithin(String key, int limit, Duration window, Instant now) {
        Bucket bucket = buckets.get(key);
        if (bucket == null || expired(bucket, window, now)) {
            buckets.put(key, new Bucket(now, 1, window));
            return;
        }
        if (bucket.attempts >= limit) {
            throw rateLimited();
        }
        buckets.put(key, new Bucket(bucket.windowStart, bucket.attempts + 1, window));
    }

    private void purgeExpired(Instant now) {
        Iterator<Map.Entry<String, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Bucket bucket = iterator.next().getValue();
            if (expired(bucket, bucket.window, now)) {
                iterator.remove();
            }
        }
    }

    private static boolean expired(Bucket bucket, Duration window, Instant now) {
        return !now.isBefore(bucket.windowStart.plus(window));
    }

    private void evictOverflow() {
        while (buckets.size() > properties.getMaxTrackedKeys()) {
            Iterator<String> iterator = buckets.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static String accountKey(Long accountId) {
        return accountId == null ? "missing" : Long.toString(accountId);
    }

    private static String ipKey(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        String normalized = remoteAddress.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private static BusinessException rateLimited() {
        return new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "EMAIL_VERIFICATION_RATE_LIMITED",
                "Có quá nhiều yêu cầu xác thực email. Vui lòng thử lại sau.");
    }

    private record Bucket(Instant windowStart, int attempts, Duration window) {}
}
