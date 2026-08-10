package com.zanh.route_sharing.repository.sharedroute.eligibility;

import com.zanh.route_sharing.repository.sharedroute.eligibility.model.CurrentOperationalEligibility;

import java.time.LocalDate;

public interface OperationalEligibilityRepository {

    CurrentOperationalEligibility evaluate(
            Long actorId,
            Long routeId,
            Long schoolId,
            LocalDate routeTravelDate);
}
