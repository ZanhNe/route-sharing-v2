package com.zanh.route_sharing.service.evidence;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EvidenceUploadLimitPolicy {
    private final MultipartProperties multipartProperties;

    public EvidenceUploadLimitPolicy(MultipartProperties multipartProperties) {
        this.multipartProperties = multipartProperties;
    }

    public long configuredMaxFileBytes() {
        long bytes = multipartProperties.getMaxFileSize().toBytes();
        if (bytes <= 0) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVIDENCE_CONTEXT_INVARIANT_VIOLATION", "Cấu hình giới hạn upload không hợp lệ.");
        }
        return bytes;
    }

    public void requireWithinLimit(long bytes) {
        if (bytes <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EVIDENCE_CONTENT_INVALID",
                    "Tệp bằng chứng không được rỗng.");
        }
        if (bytes > configuredMaxFileBytes()) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
                    "Tệp tải lên vượt quá dung lượng cho phép.");
        }
    }
}
