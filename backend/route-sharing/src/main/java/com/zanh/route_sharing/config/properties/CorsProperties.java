package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    @AssertTrue(message = "app.cors.allowed-origins phải chứa ít nhất 1 origin hợp lệ")
    public boolean isAllowedOriginsValid() {
        return WebSocketProperties.areValidOrigins(allowedOrigins);
    }
}
