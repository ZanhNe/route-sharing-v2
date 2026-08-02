package com.zanh.route_sharing.dto.sharedroute;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RouteEndpointRequest(
                @NotNull(message = "Vĩ độ không được để trống.") @DecimalMin(value = "-90.0", message = "Vĩ độ phải lớn hơn hoặc bằng -90.") @DecimalMax(value = "90.0", message = "Vĩ độ phải nhỏ hơn hoặc bằng 90.") BigDecimal latitude,

                @NotNull(message = "Kinh độ không được để trống.") @DecimalMin(value = "-180.0", message = "Kinh độ phải lớn hơn hoặc bằng -180.") @DecimalMax(value = "180.0", message = "Kinh độ phải nhỏ hơn hoặc bằng 180.") BigDecimal longitude,

                @NotBlank(message = "Địa chỉ không được để trống.") @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự.") String address) {
}
