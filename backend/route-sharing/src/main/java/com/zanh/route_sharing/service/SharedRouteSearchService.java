package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;

public interface SharedRouteSearchService {

    SharedRouteSearchResult search(
            Long actorUserId,
            SearchSharedRoutesRequest request,
            int page,
            int size);
}
