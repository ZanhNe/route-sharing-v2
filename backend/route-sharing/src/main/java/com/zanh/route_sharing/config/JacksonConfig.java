package com.zanh.route_sharing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }

    @PostConstruct
    public void customizeObjectMapper() {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(jtsModule());
    }
}