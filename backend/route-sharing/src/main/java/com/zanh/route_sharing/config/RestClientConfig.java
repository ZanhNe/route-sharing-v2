package com.zanh.route_sharing.config;

import com.zanh.route_sharing.config.properties.GoongProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {
    @Bean
    @Qualifier("goongRestClient")
    RestClient goongRestClient(RestClient.Builder builder, GoongProperties properties) {
        return builder.clone()
                .baseUrl(properties.getBaseUrl().toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
