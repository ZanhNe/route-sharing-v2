package com.zanh.route_sharing.dto.riderequest;

import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRideRequestRequest(
        @NotNull(message = "schoolId không được để trống.")
        @Positive(message = "schoolId phải là số dương.")
        Long schoolId,

        @NotNull(message = "Điểm đón không được để trống.")
        @Valid
        RouteEndpointRequest pickup,

        @NotNull(message = "Điểm đến hành khách không được để trống.")
        @Valid
        RouteEndpointRequest passengerDestination,

        @NotNull(message = "Mức hỗ trợ đề nghị không được để trống.")
        @PositiveOrZero(message = "Mức hỗ trợ đề nghị không được âm.")
        @Digits(integer = 13, fraction = 2,
                message = "Mức hỗ trợ đề nghị tối đa 13 chữ số nguyên và 2 chữ số thập phân.")
        BigDecimal proposedSupportAmount,

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
        String note) {

    public CreateRideRequestRequest {
        pickup = normalizePoint(pickup);
        passengerDestination = normalizePoint(passengerDestination);
        note = normalizeNullable(note);
    }

    private static RouteEndpointRequest normalizePoint(RouteEndpointRequest point) {
        if (point == null || point.address() == null) {
            return point;
        }
        return new RouteEndpointRequest(
                point.latitude(),
                point.longitude(),
                point.address().trim());
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
