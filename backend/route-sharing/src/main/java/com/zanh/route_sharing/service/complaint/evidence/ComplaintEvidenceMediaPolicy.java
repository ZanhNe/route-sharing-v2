package com.zanh.route_sharing.service.complaint.evidence;

import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ComplaintEvidenceMediaPolicy {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Map<String, LoaiTepMinhChung> ALLOWED = Map.ofEntries(
            Map.entry("image/jpeg", LoaiTepMinhChung.IMAGE),
            Map.entry("image/png", LoaiTepMinhChung.IMAGE),
            Map.entry("image/webp", LoaiTepMinhChung.IMAGE),
            Map.entry("video/mp4", LoaiTepMinhChung.VIDEO),
            Map.entry("video/webm", LoaiTepMinhChung.VIDEO),
            Map.entry("application/pdf", LoaiTepMinhChung.DOCUMENT),
            Map.entry("text/plain", LoaiTepMinhChung.DOCUMENT),
            Map.entry(DOCX, LoaiTepMinhChung.DOCUMENT),
            Map.entry("audio/mpeg", LoaiTepMinhChung.AUDIO),
            Map.entry("audio/wav", LoaiTepMinhChung.AUDIO),
            Map.entry("audio/x-wav", LoaiTepMinhChung.AUDIO),
            Map.entry("audio/mp4", LoaiTepMinhChung.AUDIO));

    public LoaiTepMinhChung requireAllowed(String mediaType) {
        LoaiTepMinhChung category = ALLOWED.get(mediaType);
        if (category == null) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EVIDENCE_MEDIA_TYPE_NOT_ALLOWED",
                    "Kiểu nội dung bằng chứng không được hỗ trợ.");
        }
        return category;
    }
}
