package com.zanh.route_sharing.integration.goong;

import org.springframework.util.MultiValueMap;

public interface GoongApiGateway {

    <T> T get(
            String relativePath,
            MultiValueMap<String, String> queryParameters,
            Class<T> responseType);
}