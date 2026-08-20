package com.zanh.route_sharing.dto.complaint.review;
import com.zanh.route_sharing.dto.response.PageMeta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record ComplaintLocationEvidencePageResponse(List<Item> items, PageMeta page) {
    public record Item(Long locationId, long sequence, Instant observedAt, Instant receivedAt,
                       BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters) {}
}
