package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

import com.zanh.route_sharing.utils.PaginationPolicy;
import java.util.List;

public record PassengerRideRequestPageSnapshot(
        List<PassengerRideRequestSummaryRow> rows,
        long totalElements,
        int page,
        int size) {

    public PassengerRideRequestPageSnapshot {
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements phải lớn hơn hoặc bằng 0.");
        }
        PaginationPolicy.requireValid(page, size);
    }
}
