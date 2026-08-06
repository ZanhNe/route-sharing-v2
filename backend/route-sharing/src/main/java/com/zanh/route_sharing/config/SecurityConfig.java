package com.zanh.route_sharing.config;

import com.zanh.route_sharing.config.properties.CorsProperties;
import com.zanh.route_sharing.config.properties.InternalPortalProperties;
import com.zanh.route_sharing.security.CustomUserDetailsService;
import com.zanh.route_sharing.security.InternalSessionSecurityFilter;
import com.zanh.route_sharing.security.JwtAuthenticationFilter;
import com.zanh.route_sharing.security.RestAccessDeniedHandler;
import com.zanh.route_sharing.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final InternalSessionSecurityFilter internalSessionSecurityFilter;
        private final CustomUserDetailsService userDetailsService;
        private final RestAuthenticationEntryPoint authenticationEntryPoint;
        private final RestAccessDeniedHandler accessDeniedHandler;
        private final CorsProperties corsProperties;
        private final InternalPortalProperties internalPortalProperties;

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                return provider;
        }

        @Bean
        AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
                return new ProviderManager(authenticationProvider);
        }

        @Bean
        CorsConfigurationSource apiCorsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With"));
                configuration.setExposedHeaders(List.of(
                                "Location",
                                "Content-Disposition"));

                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }

        @Bean
        @Order(1)
        SecurityFilterChain apiSecurityFilterChain(
                        HttpSecurity http,
                        AuthenticationProvider authenticationProvider,
                        @Qualifier("apiCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource)
                        throws Exception {
                http
                                .securityMatcher("/api/**")
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .anyRequest().authenticated())
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain webSocketHandshakeSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/ws-ridesharing", "/ws-ridesharing/**")

                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
                return http.build();
        }

        @Bean
        @Order(3)
        SecurityFilterChain internalSecurityFilterChain(HttpSecurity http,
                        AuthenticationProvider authenticationProvider) throws Exception {
                String requiredAuthority = internalPortalProperties.getRequiredAuthority().trim();
                http
                                .securityMatcher("/internal/**", "/assets/**", "/css/**", "/js/**", "/images/**")
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(
                                                                "/internal/login",
                                                                "/internal/access-denied",
                                                                "/assets/**", "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .anyRequest().hasAuthority(requiredAuthority))
                                .formLogin(form -> form
                                                .loginPage("/internal/login")
                                                .loginProcessingUrl("/internal/login")
                                                .defaultSuccessUrl("/internal", true)
                                                .failureUrl("/internal/login?error")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/internal/logout")
                                                .logoutSuccessUrl("/internal/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID"))
                                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/internal/access-denied"))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .sessionFixation(fixation -> fixation.migrateSession()))

                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(internalSessionSecurityFilter, AuthorizationFilter.class);
                return http.build();
        }

        @Bean
        @Order(4)
        SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
                http.authorizeHttpRequests(authorize -> authorize
                                .requestMatchers("/error").permitAll()
                                .anyRequest().denyAll());
                return http.build();
        }
}
