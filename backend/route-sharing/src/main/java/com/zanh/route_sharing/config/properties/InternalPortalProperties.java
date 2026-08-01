package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.internal-portal")
public class InternalPortalProperties {
    @NotBlank(message = "app.internal-portal.required-authority không được để trống")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{2,149}", message = "app.internal-portal.required-authority phải là 3-150 ký tự, bắt đầu bằng chữ cái và chỉ chứa chữ cái, số hoặc gạch dưới")
    private String requiredAuthority = "ACCESS_INTERNAL_PORTAL";
}
