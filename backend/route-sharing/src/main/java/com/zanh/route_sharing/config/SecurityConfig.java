package com.zanh.route_sharing.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.zanh.route_sharing.security.CustomUserDetailsService;
import com.zanh.route_sharing.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Thuật toán mã hóa mật khẩu (BCrypt là chuẩn an toàn nhất hiện nay)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provider kết nối giữa UserDetailsService (DB) và PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Quản lý xác thực (dùng trong AuthController khi user gọi API Login)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cấu hình CORS để Frontend (React/Vite) có thể gọi API mà không bị chặn
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Môi trường Dev: Cho phép mọi origin (*). Khi lên Production sẽ sửa thành URL
        // thật của React.
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Cho phép các phương thức HTTP cơ bản
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Cho phép Frontend gửi lên bất kỳ Header nào
        configuration.setAllowedHeaders(List.of("*"));

        // Bắt buộc = true nếu Frontend có sử dụng Credentials (token/cookie gắn ngầm)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng luật CORS này cho toàn bộ endpoint (/api/**)
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * BỘ LỌC BẢO MẬT CHÍNH CỦA HỆ THỐNG
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Áp dụng CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Tắt CSRF (Vì API dùng JWT Stateless nên không sợ lỗi bảo mật CSRF)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Thiết lập Stateless (Không lưu Session trên Server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Phân luồng các API (Routing Rules)
                .authorizeHttpRequests(auth -> auth
                        // - Mở tự do API Đăng nhập, Đăng ký, Quên mật khẩu
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // - Mở tự do cổng WebSocket để Client handshake bắt tay
                        .requestMatchers("/ws-ridesharing/**").permitAll()
                        // - Toàn bộ các API nghiệp vụ khác ĐỀU PHẢI CÓ TOKEN HỢP LỆ
                        .anyRequest().authenticated())

                // 5. Khai báo Provider
                .authenticationProvider(authenticationProvider())

                // 6. Chèn JWT Filter chặn trước lớp Filter mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}