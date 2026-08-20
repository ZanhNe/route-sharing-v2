package com.zanh.route_sharing.service.evidence;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EvidenceFilenamePolicy {
    public String normalize(String value) {
        String input = value == null ? "" : value.replace('\\', '/');
        int slash = input.lastIndexOf('/');
        if (slash >= 0)
            input = input.substring(slash + 1);
        StringBuilder safe = new StringBuilder();
        input.codePoints()
                .filter(cp -> !Character.isISOControl(cp) && cp != '\r' && cp != '\n')
                .forEach(safe::appendCodePoint);
        String normalized = safe.toString().trim();
        if (normalized.isEmpty())
            normalized = "evidence";
        if (normalized.length() > 255) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Tên tệp không được vượt quá 255 ký tự.");
        }
        return normalized;
    }
}
