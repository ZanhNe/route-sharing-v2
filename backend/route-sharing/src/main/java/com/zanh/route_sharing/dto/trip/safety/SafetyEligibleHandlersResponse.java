package com.zanh.route_sharing.dto.trip.safety;

import java.util.List;

public record SafetyEligibleHandlersResponse(List<Item> items, SafetyPageMeta page) {
    public record Item(Long userId, String fullName, boolean currentHandler) {}
}
