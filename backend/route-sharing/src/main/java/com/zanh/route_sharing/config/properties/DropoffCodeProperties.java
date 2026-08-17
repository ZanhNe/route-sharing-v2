package com.zanh.route_sharing.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.dropoff-code")
public class DropoffCodeProperties {
    private String activeKeyVersion = "v1";
    private Map<String, String> keys = new LinkedHashMap<>();
}
