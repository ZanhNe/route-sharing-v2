package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "goong")
public class GoongProperties {
    @NotBlank(message = "goong.api-key không được để trống")
    private String apiKey;
    @NotNull(message = "goong.base-url không được để trống")
    private URI baseUrl = URI.create("https://rsapi.goong.io");

    @NotBlank(message = "goong.directions-path không được để trống")
    @Pattern(regexp = "/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*", message = "goong.directions-path phải là relative path hợp lệ")
    private String directionsPath = "/v2/direction";

    @NotBlank(message = "goong.geocoding-path không được để trống")
    @Pattern(regexp = "/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*", message = "goong.geocoding-path phải là relative path hợp lệ")
    private String geocodingPath = "/Geocode";

    @NotNull(message = "goong.waypoint-snap-tolerance-meters không được để trống")
    @DecimalMin(value = "0.01", message = "goong.waypoint-snap-tolerance-meters phải lớn hơn 0")
    private BigDecimal waypointSnapToleranceMeters = new BigDecimal("100.00");

    @NotNull(message = "goong.duplicate-waypoint-tolerance-meters không được để trống")
    @DecimalMin(value = "0.0", message = "goong.duplicate-waypoint-tolerance-meters không được âm")
    private BigDecimal duplicateWaypointToleranceMeters = new BigDecimal("2.00");

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDirectionsPath() {
        return directionsPath;
    }

    public void setDirectionsPath(String directionsPath) {
        this.directionsPath = directionsPath;
    }

    public String getGeocodingPath() {
        return geocodingPath;
    }

    public void setGeocodingPath(String geocodingPath) {
        this.geocodingPath = geocodingPath;
    }

    public BigDecimal getWaypointSnapToleranceMeters() {
        return waypointSnapToleranceMeters;
    }

    public void setWaypointSnapToleranceMeters(BigDecimal waypointSnapToleranceMeters) {
        this.waypointSnapToleranceMeters = waypointSnapToleranceMeters;
    }

    public BigDecimal getDuplicateWaypointToleranceMeters() {
        return duplicateWaypointToleranceMeters;
    }

    public void setDuplicateWaypointToleranceMeters(BigDecimal duplicateWaypointToleranceMeters) {
        this.duplicateWaypointToleranceMeters = duplicateWaypointToleranceMeters;
    }

    @AssertTrue(message = "goong.duplicate-waypoint-tolerance-meters phải nhỏ hơn hoặc bằng waypoint snap tolerance")
    public boolean isDuplicateToleranceWithinSnapTolerance() {
        if (duplicateWaypointToleranceMeters == null || waypointSnapToleranceMeters == null) {
            return true;
        }
        return duplicateWaypointToleranceMeters.compareTo(waypointSnapToleranceMeters) <= 0;
    }

    @AssertTrue(message = "goong.base-url phải là một HTTPS origin không có thông tin xác thực, query hoặc fragment")
    public boolean isSecureBaseUrl() {
        if (baseUrl == null
                || !"https".equalsIgnoreCase(baseUrl.getScheme())
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null) {
            return false;
        }
        String path = baseUrl.getPath();
        return path == null || path.isEmpty() || "/".equals(path);
    }
}
