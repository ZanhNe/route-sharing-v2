package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.sharedroute.search.SharedRouteSearchRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class RecordingSharedRouteSearchRepository implements SharedRouteSearchRepository {

    private Optional<SharedRouteMatchingContext> context =
            Optional.of(SharedRouteMatchingContextMother.standardConfiguration());
    private SharedRouteSearchPage page = new SharedRouteSearchPage(List.of(), 0L);

    private int contextQueryCount;
    private int searchQueryCount;
    private Long lastActorUserId;
    private Long lastSchoolId;
    private LocalDate lastRequestedTravelDate;
    private SharedRouteSearchCriteria lastCriteria;

    public RecordingSharedRouteSearchRepository withContext(
            SharedRouteMatchingContext context) {
        this.context = Optional.ofNullable(context);
        return this;
    }

    public RecordingSharedRouteSearchRepository withoutEligibleContext() {
        this.context = Optional.empty();
        return this;
    }

    public RecordingSharedRouteSearchRepository withPage(
            SharedRouteSearchPage page) {
        this.page = page;
        return this;
    }

    @Override
    public Optional<SharedRouteMatchingContext> findSearchContext(
            Long actorUserId,
            Long schoolId,
            LocalDate requestedTravelDate) {
        contextQueryCount++;
        lastActorUserId = actorUserId;
        lastSchoolId = schoolId;
        lastRequestedTravelDate = requestedTravelDate;
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

    public LocalDate lastRequestedTravelDate() {
        return lastRequestedTravelDate;
    }

    /**
     * Alias tạm để những test cũ chưa đổi tên không bị gãy ngay.
     */
    public LocalDate lastMembershipDate() {
        return lastRequestedTravelDate;
    }

    public SharedRouteSearchCriteria lastCriteria() {
        return lastCriteria;
    }
}
