package com.zanh.route_sharing.dto.complaint.review;

import com.zanh.route_sharing.dto.response.PageMeta;
import java.util.List;

public record EligibleReviewerPageResponse(List<Item> items, PageMeta page) {
    public record Item(Long reviewerId, String displayName) {
    }
}
