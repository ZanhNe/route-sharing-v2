package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.dto.auth.LoginRequest;
import com.zanh.route_sharing.dto.auth.RefreshTokenRequest;
import com.zanh.route_sharing.dto.auth.session.AuthSessionResponse;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import com.zanh.route_sharing.security.AuthTokenService;
import com.zanh.route_sharing.security.ClientRequestInfo;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.security.IssuedToken;
import com.zanh.route_sharing.security.JwtAccessClaims;
import com.zanh.route_sharing.security.JwtUtil;
import com.zanh.route_sharing.security.OnboardingAccessTokenService;
import com.zanh.route_sharing.security.OnboardingStep;
import com.zanh.route_sharing.security.TokenPair;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationSessionServiceImpl implements AuthenticationSessionService {
    private final StateAwareCredentialAuthenticator credentialAuthenticator;
    private final OnboardingAccessTokenService onboardingTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final JwtUtil jwtUtil;
    private final NguoiDungSecurityRepository accountRepository;

    public AuthenticationSessionServiceImpl(
            StateAwareCredentialAuthenticator credentialAuthenticator,
            OnboardingAccessTokenService onboardingTokenService,
            AuthenticationManager authenticationManager,
            AuthTokenService authTokenService,
            JwtUtil jwtUtil,
            NguoiDungSecurityRepository accountRepository) {
        this.credentialAuthenticator = credentialAuthenticator;
        this.onboardingTokenService = onboardingTokenService;
        this.authenticationManager = authenticationManager;
        this.authTokenService = authTokenService;
        this.jwtUtil = jwtUtil;
        this.accountRepository = accountRepository;
    }

    @Override
    public AuthSessionResponse login(LoginRequest request, ClientRequestInfo clientRequestInfo) {
        VerifiedAccountCredential account = credentialAuthenticator.authenticate(request.email(), request.password());
        TrangThaiTaiKhoan status = account.accountStatus();

        if (status == TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL) {
            IssuedToken token = onboardingTokenService.issue(
                    account.accountId(),
                    account.schoolEmail(),
                    status,
                    account.securityVersion(),
                    OnboardingStep.VERIFY_EMAIL);
            return AuthSessionResponse.onboarding(account, token, "VERIFY_EMAIL");
        }

        if (status == TrangThaiTaiKhoan.CHO_DUYET_HO_SO) {
            IssuedToken token = onboardingTokenService.issue(
                    account.accountId(),
                    account.schoolEmail(),
                    status,
                    account.securityVersion(),
                    OnboardingStep.COMPLETE_PROFILE);
            return AuthSessionResponse.onboarding(account, token, "COMPLETE_PROFILE");
        }

        if (status == TrangThaiTaiKhoan.ACTIVE) {
            // Preserve the accepted operational authentication pipeline for FULL sessions.
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim(),
                            request.password()));
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            TokenPair pair = authTokenService.issue(principal, clientRequestInfo);
            return AuthSessionResponse.full(account, pair);
        }

        throw new IllegalStateException(
                "Trạng thái tài khoản không được hỗ trợ sau credential authentication: " + status);
    }

    @Override
    public AuthSessionResponse refresh(RefreshTokenRequest request, ClientRequestInfo clientRequestInfo) {
        TokenPair pair = authTokenService.rotate(request.refreshToken(), clientRequestInfo);
        JwtAccessClaims accessClaims = jwtUtil.parseAccessToken(pair.accessToken());
        NguoiDung account = accountRepository.findPrincipalById(accessClaims.userId())
                .orElseThrow(() -> new IllegalStateException("Tài khoản biến mất sau khi refresh token được rotate."));
        return AuthSessionResponse.full(toVerifiedAccount(account), pair);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        authTokenService.revoke(request.refreshToken());
    }

    private static VerifiedAccountCredential toVerifiedAccount(NguoiDung account) {
        return new VerifiedAccountCredential(
                account.getId(),
                account.getHoTen(),
                account.getEmailTruong(),
                account.getTrangThaiTaiKhoan(),
                account.getEmailDaXacThucLuc(),
                account.getSecurityVersion() == null ? 0L : account.getSecurityVersion());
    }
}
