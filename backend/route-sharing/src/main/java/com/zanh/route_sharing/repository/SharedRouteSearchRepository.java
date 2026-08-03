package com.zanh.route_sharing.repository;

import java.time.LocalDate;
import java.util.Optional;

public interface SharedRouteSearchRepository {

    Optional<SharedRouteSearchContext> findSearchContext(
            Long actorUserId,
            Long schoolId,
            LocalDate membershipDate);

    SharedRouteSearchPage search(SharedRouteSearchCriteria criteria);
}
