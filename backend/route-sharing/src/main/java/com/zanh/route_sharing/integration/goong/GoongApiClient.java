package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoongApiClient implements GoongApiGateway {

    private static final Logger log = LoggerFactory.getLogger(GoongApiClient.class);
    private final RestClient restClient;
    private final GoongProperties properties;

    public GoongApiClient(@Qualifier("goongRestClient") RestClient restClient,
            GoongProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
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
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_INVALID_RESPONSE",
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
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_UNAVAILABLE",
                    "Dịch vụ bản đồ không xử lý được yêu cầu.");
        } catch (ResourceAccessException exception) {
            log.warn("Không thể kết nối dịch vụ bản đồ tại {}", relativePath, exception);
            if (isTimeout(exception)) {
                throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "MAP_PROVIDER_TIMEOUT",
                        "Dịch vụ bản đồ phản hồi quá thời gian cho phép.");
            }
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_UNAVAILABLE",
                    "Không thể kết nối dịch vụ bản đồ.");
        }
    }

    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void validateRelativePath(String relativePath) {
        if (relativePath == null
                || !relativePath.matches("/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")
                || relativePath.contains("..")) {
            throw new IllegalArgumentException("Đường dẫn API không hợp lệ");
        }
    }
}
