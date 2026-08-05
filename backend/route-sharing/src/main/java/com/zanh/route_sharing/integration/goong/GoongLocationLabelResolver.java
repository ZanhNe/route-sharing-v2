package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.LocationLabelResolver;
import com.zanh.route_sharing.service.riderequest.model.LocationLabel;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class GoongLocationLabelResolver implements LocationLabelResolver {

    private final GoongApiGateway gateway;
    private final GoongProperties properties;

    public GoongLocationLabelResolver(
            GoongApiGateway gateway,
            GoongProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    public LocationLabel resolve(GeoCoordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate không được trống");
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("latlng", coordinate.latitude().toPlainString()
                + ","
                + coordinate.longitude().toPlainString());
        GoongGeocodingResponse response = gateway.get(
                properties.getGeocodingPath(),
                query,
                GoongGeocodingResponse.class);

        if (response == null
                || response.status() == null
                || !"OK".equalsIgnoreCase(response.status())
                || response.results() == null) {
            throw invalidResponse();
        }

        return response.results().stream()
                .filter(result -> result != null
                        && result.formattedAddress() != null
                        && !result.formattedAddress().isBlank())
                .findFirst()
                .map(result -> toLabel(result.formattedAddress()))
                .orElseThrow(GoongLocationLabelResolver::invalidResponse);
    }

    private static LocationLabel toLabel(String address) {
        try {
            return new LocationLabel(address);
        } catch (IllegalArgumentException exception) {
            throw invalidResponse();
        }
    }

    private static BusinessException invalidResponse() {
        return new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "MAP_PROVIDER_INVALID_RESPONSE",
                "Dịch vụ bản đồ không trả về địa chỉ điểm thả hợp lệ.");
    }
}
