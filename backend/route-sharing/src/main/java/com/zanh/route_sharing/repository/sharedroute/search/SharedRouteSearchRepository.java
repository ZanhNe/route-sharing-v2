package com.zanh.route_sharing.repository.sharedroute.search;

import java.time.LocalDate;
import java.util.Optional;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;

public interface SharedRouteSearchRepository {

    Optional<SharedRouteMatchingContext> findSearchContext(
            Long actorUserId,
            Long schoolId,
            LocalDate requestedTravelDate);

    SharedRouteSearchPage search(SharedRouteSearchCriteria criteria);
}
