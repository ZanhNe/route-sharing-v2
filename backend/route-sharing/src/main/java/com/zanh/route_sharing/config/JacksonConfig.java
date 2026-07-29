package com.zanh.route_sharing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper; // Thay đổi import sang JsonMapper
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }

    @Bean
    public ObjectMapper objectMapper() {
        // Sử dụng mẫu thiết kế Builder chuẩn của Jackson 3 (JsonMapper) thay cho lớp cũ
        // của Spring
        return JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Cấu hình ngày tháng dạng ISO chuỗi
                .addModule(jtsModule()) // Đăng ký JtsModule xử lý dữ liệu PostGIS (Point, LineString)
                .build();
    }
}
