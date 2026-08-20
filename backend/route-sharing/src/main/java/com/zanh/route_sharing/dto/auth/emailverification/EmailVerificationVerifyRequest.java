package com.zanh.route_sharing.dto.auth.emailverification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationVerifyRequest(
                @NotBlank(message = "Mã xác thực không được để trống.") @Pattern(regexp = "[0-9]{6}", message = "Mã xác thực phải gồm đúng 6 chữ số ASCII.") String code) {
}
