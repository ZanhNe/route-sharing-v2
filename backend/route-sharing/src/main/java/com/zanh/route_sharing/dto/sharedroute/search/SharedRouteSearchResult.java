package com.zanh.route_sharing.dto.sharedroute.search;

import com.zanh.route_sharing.dto.response.PageMeta;

import java.util.List;

public record SharedRouteSearchResult(
        List<SharedRouteSearchItemResponse> items,
        PageMeta meta
) {
    public SharedRouteSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (meta == null) {
            throw new IllegalArgumentException("meta is required");
        }
    }
}
