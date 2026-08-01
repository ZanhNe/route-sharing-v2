package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoongApiClient {
    private final RestClient restClient;
    private final GoongProperties properties;

    public GoongApiClient(@Qualifier("goongRestClient") RestClient restClient,
            GoongProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public <T> T get(String relativePath, MultiValueMap<String, String> queryParameters, Class<T> responseType) {
        validateRelativePath(relativePath);
        if (queryParameters != null && queryParameters.keySet().stream()
                .anyMatch(name -> name != null && "api_key".equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("api_key được quản lý bởi GoongApiClient");
        }
        try {
            T body = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(relativePath).queryParam("api_key", properties.getApiKey());
                        if (queryParameters != null) {
                            queryParameters.forEach(
                                    (name, values) -> values.forEach(value -> builder.queryParam(name, value)));
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(responseType);
            if (body == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "GOONG_EMPTY_RESPONSE",
                        "Dịch vụ bản đồ trả về phản hồi rỗng.");
            }
            return body;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("Goong API trả về HTTP {} cho đường dẫn {}", status, relativePath);
            if (status == 429) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "GOONG_RATE_LIMITED",
                        "Dịch vụ bản đồ đang giới hạn số lượng yêu cầu. Vui lòng thử lại sau.");
            }
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "GOONG_API_ERROR",
                    "Dịch vụ bản đồ không xử lý được yêu cầu.");
        } catch (ResourceAccessException exception) {
            log.warn("Không thể kết nối dịch vụ bản đồ", relativePath, exception);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "GOONG_UNAVAILABLE",
                    "Không thể kết nối dịch vụ bản đồ.");
        }
    }

    private static void validateRelativePath(String relativePath) {
        if (relativePath == null
                || !relativePath.matches("/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")
                || relativePath.contains("..")) {
            throw new IllegalArgumentException("Đường dẫn API không hợp lệ");
        }
    }
}
