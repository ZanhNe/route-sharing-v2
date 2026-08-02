package com.zanh.route_sharing.dto.sharedroute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateSharedRouteRequest(
                @NotNull(message = "Điểm xuất phát không được để trống.") @Valid RouteEndpointRequest origin,

                @NotNull(message = "Điểm đích không được để trống.") @Valid RouteEndpointRequest driverDestination,

                @NotNull(message = "Thời gian khởi hành không được để trống.") @Future(message = "Thời gian khởi hành phải nằm trong tương lai.") Instant expectedDepartureTime,

                @NotNull(message = "Phương tiện không được để trống.") @Positive(message = "Mã phương tiện phải là số dương.") Long vehicleId,

                @NotNull(message = "Số ghế cung cấp không được để trống.") @Min(value = 1, message = "Số ghế cung cấp phải lớn hơn hoặc bằng 1.") Integer offeredSeats,

                @PositiveOrZero(message = "Mức hỗ trợ gợi ý không được âm.") @Digits(integer = 13, fraction = 2, message = "Mức hỗ trợ gợi ý có tối đa 13 chữ số nguyên và 2 chữ số thập phân.") BigDecimal suggestedSupportPerKm) {
}
