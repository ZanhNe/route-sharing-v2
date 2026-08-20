package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.dto.auth.LoginRequest;
import com.zanh.route_sharing.dto.auth.RefreshTokenRequest;
import com.zanh.route_sharing.dto.auth.session.AuthSessionResponse;
import com.zanh.route_sharing.security.ClientRequestInfo;

public interface AuthenticationSessionService {
    AuthSessionResponse login(LoginRequest request, ClientRequestInfo clientRequestInfo);

    AuthSessionResponse refresh(RefreshTokenRequest request, ClientRequestInfo clientRequestInfo);

    void logout(RefreshTokenRequest request);
}
