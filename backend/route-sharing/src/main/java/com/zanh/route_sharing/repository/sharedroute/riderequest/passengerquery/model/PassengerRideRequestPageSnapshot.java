package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

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
        if (page < 0) {
            throw new IllegalArgumentException("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size phải nằm trong khoảng từ 1 đến 50.");
        }
    }
}
