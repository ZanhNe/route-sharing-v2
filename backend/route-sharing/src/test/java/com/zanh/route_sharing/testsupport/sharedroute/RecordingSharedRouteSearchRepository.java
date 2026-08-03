package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.repository.SharedRouteSearchContext;
import com.zanh.route_sharing.repository.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.SharedRouteSearchRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class RecordingSharedRouteSearchRepository implements SharedRouteSearchRepository {

    private Optional<SharedRouteSearchContext> context = Optional
            .of(SharedRouteSearchContextMother.standardConfiguration());
    private SharedRouteSearchPage page = new SharedRouteSearchPage(List.of(), 0L);

    private int contextQueryCount;
    private int searchQueryCount;
    private Long lastActorUserId;
    private Long lastSchoolId;
    private LocalDate lastMembershipDate;
    private SharedRouteSearchCriteria lastCriteria;

    public RecordingSharedRouteSearchRepository withContext(SharedRouteSearchContext context) {
        this.context = Optional.ofNullable(context);
        return this;
    }

    public RecordingSharedRouteSearchRepository withoutEligibleContext() {
        this.context = Optional.empty();
        return this;
    }

    public RecordingSharedRouteSearchRepository withPage(SharedRouteSearchPage page) {
        this.page = page;
        return this;
    }

    @Override
    public Optional<SharedRouteSearchContext> findSearchContext(
            Long actorUserId,
            Long schoolId,
            LocalDate membershipDate) {
        contextQueryCount++;
        lastActorUserId = actorUserId;
        lastSchoolId = schoolId;
        lastMembershipDate = membershipDate;
        return context;
    }

    @Override
    public SharedRouteSearchPage search(SharedRouteSearchCriteria criteria) {
        searchQueryCount++;
        lastCriteria = criteria;
        return page;
    }

    public int contextQueryCount() {
        return contextQueryCount;
    }

    public int searchQueryCount() {
        return searchQueryCount;
    }

    public Long lastActorUserId() {
        return lastActorUserId;
    }

    public Long lastSchoolId() {
        return lastSchoolId;
    }

    public LocalDate lastMembershipDate() {
        return lastMembershipDate;
    }

    public SharedRouteSearchCriteria lastCriteria() {
        return lastCriteria;
    }
}
