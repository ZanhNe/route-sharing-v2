package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.auth.LoginRequest;
import com.zanh.route_sharing.dto.auth.RefreshTokenRequest;
import com.zanh.route_sharing.dto.auth.TokenResponse;
import com.zanh.route_sharing.security.AuthTokenService;
import com.zanh.route_sharing.security.ClientRequestInfo;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.security.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ApiAuthController {
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return tokenResponse(tokenService.issue(principal, clientInfo(servletRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                  HttpServletRequest servletRequest) {
        return tokenResponse(tokenService.rotate(request.refreshToken(), clientInfo(servletRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<TokenResponse> tokenResponse(TokenPair pair) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(TokenResponse.from(pair));
    }

    private static ClientRequestInfo clientInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }
}
