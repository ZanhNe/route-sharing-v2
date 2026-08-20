package com.zanh.route_sharing.service.iam.registration;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class RegistrationEmailPolicy {

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasUsableDomainConfiguration(Set<String> configuredDomains) {
        if (configuredDomains == null || configuredDomains.isEmpty()) {
            return false;
        }
        return configuredDomains.stream().anyMatch(domain -> normalizeDomain(domain) != null);
    }

    public void requireAllowedDomain(String normalizedEmail, Set<String> configuredDomains) {
        String emailDomain = emailDomain(normalizedEmail);
        boolean allowed = configuredDomains != null && configuredDomains.stream()
                .map(this::normalizeDomain)
                .filter(domain -> domain != null)
                .anyMatch(emailDomain::equals);
        if (!allowed) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED",
                    "Email trường không thuộc tên miền được phép của trường đã chọn.");
        }
    }

    private String emailDomain(String normalizedEmail) {
        if (normalizedEmail == null) {
            return "";
        }
        int at = normalizedEmail.lastIndexOf('@');
        return at < 0 || at == normalizedEmail.length() - 1 ? "" : normalizedEmail.substring(at + 1);
    }

    private String normalizeDomain(String domain) {
        if (domain == null) {
            return null;
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.contains("@") || normalized.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return normalized;
    }
}
