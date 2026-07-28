package com.zanh.route_sharing.utils;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.zanh.route_sharing.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalApiUtil {

    private final RestTemplate restTemplate;

    public <T> T sendGetRequest(String baseUrl, Map<String, Object> params, Class<T> responseType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
            if (params != null) {
                params.forEach(builder::queryParam);
            }

            String finalUrl = builder.toUriString();
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, responseType);
            return response.getBody();

        } catch (RestClientResponseException e) {
            log.error("API Error Response Body: {}", e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối API đối tác: " + e.getStatusText());
        } catch (Exception e) {
            log.error("API Call Exception: ", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi xử lý API.");
        }
    }

    public <T, R> R sendPostRequest(String baseUrl, T requestBody, Class<R> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<R> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, responseType);
            return response.getBody();

        } catch (RestClientResponseException e) {
            log.error("API Error Response Body: {}", e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối API đối tác: " + e.getStatusText());
        } catch (Exception e) {
            log.error("API Call Exception: ", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi xử lý API.");
        }
    }

    public <T, R> R sendPutRequest(String baseUrl, T requestBody, Class<R> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<R> response = restTemplate.exchange(baseUrl, HttpMethod.PUT, entity, responseType);
            return response.getBody();

        } catch (RestClientResponseException e) {
            log.error("API Error Response Body: {}", e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối API đối tác: " + e.getStatusText());
        } catch (Exception e) {
            log.error("API Call Exception: ", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi xử lý API.");
        }
    }

    public <T> T sendDeleteRequest(String baseUrl, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(baseUrl, HttpMethod.DELETE, entity, responseType);
            return response.getBody();

        } catch (RestClientResponseException e) {
            log.error("API Error Response Body: {}", e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối API đối tác: " + e.getStatusText());
        } catch (Exception e) {
            log.error("API Call Exception: ", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi xử lý API.");
        }
    }
}