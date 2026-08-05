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
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {
    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://localhost:8080"));

    @AssertTrue(message = "app.websocket.allowed-origins không được để trống")
    public boolean isAllowedOriginsValid() {
        return areValidOrigins(allowedOrigins);
    }

    static boolean areValidOrigins(List<String> origins) {
        return OriginListValidator.isValid(origins);
    }

}
