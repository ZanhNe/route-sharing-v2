package com.zanh.route_sharing.security;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.RefreshTokenSession;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.RefreshTokenSessionRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenSessionRepository refreshTokenRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public TokenPair issue(CustomUserDetails principal, ClientRequestInfo clientInfo) {
        requireLoginAllowed(principal);
        IssuedToken access = jwtUtil.issueAccessToken(principal);
        IssuedToken refresh = jwtUtil.issueRefreshToken(principal);
        saveRefreshSession(principal, refresh, clientInfo);
        return pair(access, refresh);
    }

    public TokenPair rotate(String rawRefreshToken, ClientRequestInfo clientInfo) {
        JwtRefreshClaims claims = parseRefresh(rawRefreshToken);
        try {
            return Objects.requireNonNull(
                    transactionTemplate.execute(status -> rotateInTransaction(rawRefreshToken, claims, clientInfo)));
        } catch (RefreshTokenReuseDetectedException reuse) {
            Instant compromisedAt = TimePolicy.now(clock);
            transactionTemplate.executeWithoutResult(
                    status -> refreshTokenRepository.revokeAllActiveByUserId(reuse.userId(), compromisedAt));
            throw invalidRefreshToken();
        }
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        try {
            JwtRefreshClaims claims = jwtUtil.parseRefreshToken(rawRefreshToken);
            refreshTokenRepository.findForUpdateByJti(claims.jwtId()).ifPresent(session -> {
                if (session.getRevokedAt() == null
                        && RefreshTokenHasher.matches(rawRefreshToken, session.getTokenHash())) {
                    session.setRevokedAt(TimePolicy.now(clock));
                }
            });
        } catch (JwtException ignored) {

        }
    }

    @Transactional
    public int revokeAllForUser(Long userId) {
        return refreshTokenRepository.revokeAllActiveByUserId(userId, TimePolicy.now(clock));
    }

    private TokenPair rotateInTransaction(String rawRefreshToken,
            JwtRefreshClaims claims,
            ClientRequestInfo clientInfo) {
        RefreshTokenSession current = refreshTokenRepository.findForUpdateByJti(claims.jwtId())
                .orElseThrow(AuthTokenService::invalidRefreshToken);
        Instant now = TimePolicy.now(clock);
        boolean tokenHashMatches = RefreshTokenHasher.matches(rawRefreshToken, current.getTokenHash());

        if (current.getRevokedAt() != null) {

            if (tokenHashMatches && current.getReplacedByJti() != null) {
                throw new RefreshTokenReuseDetectedException(claims.userId());
            }
            throw invalidRefreshToken();
        }

        if (!current.getExpiresAt().isAfter(now)
                || !current.getNguoiDung().getId().equals(claims.userId())
                || !tokenHashMatches) {
            throw invalidRefreshToken();
        }

        CustomUserDetails principal = userDetailsService.loadUserById(claims.userId());
        requireLoginAllowed(principal);

        IssuedToken access = jwtUtil.issueAccessToken(principal);
        IssuedToken refresh = jwtUtil.issueRefreshToken(principal);

        current.setRevokedAt(now);
        current.setReplacedByJti(refresh.jwtId());
        saveRefreshSession(principal, refresh, clientInfo);
        return pair(access, refresh);
    }

    private void saveRefreshSession(CustomUserDetails principal,
            IssuedToken refresh,
            ClientRequestInfo clientInfo) {
        RefreshTokenSession session = RefreshTokenSession.builder()
                .jti(refresh.jwtId())
                .tokenHash(RefreshTokenHasher.sha256(refresh.value()))
                .expiresAt(refresh.expiresAt())
                .ipAddress(clientInfo == null ? null : clientInfo.ipAddress())
                .userAgent(clientInfo == null ? null : clientInfo.userAgent())
                .nguoiDung(nguoiDungRepository.getReferenceById(principal.getId()))
                .build();
        refreshTokenRepository.save(session);
    }

    private JwtRefreshClaims parseRefresh(String token) {
        try {
            return jwtUtil.parseRefreshToken(token);
        } catch (JwtException exception) {
            throw invalidRefreshToken();
        }
    }

    private static void requireLoginAllowed(CustomUserDetails principal) {
        if (principal.getTrangThaiTaiKhoan() == TrangThaiTaiKhoan.SUSPENDED
                || principal.getTrangThaiTaiKhoan() == TrangThaiTaiKhoan.BANNED) {
            throw new LockedException("Tài khoản đã bị khóa.");
        }
        if (!principal.isEnabled() || !principal.isAccountNonExpired()) {
            throw new DisabledException("Tài khoản không hoạt động hoặc đã hết hạn.");
        }
    }

    private static TokenPair pair(IssuedToken access, IssuedToken refresh) {
        return new TokenPair("Bearer", access.value(), access.expiresAt(), refresh.value(), refresh.expiresAt());
    }

    private static BusinessException invalidRefreshToken() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Refresh token không hợp lệ hoặc đã hết hiệu lực.");
    }

    private static final class RefreshTokenReuseDetectedException extends RuntimeException {
        private final Long userId;

        private RefreshTokenReuseDetectedException(Long userId) {
            super("Refresh token đã bị thu hồi", null, false, false);
            this.userId = userId;
        }

        private Long userId() {
            return userId;
        }
    }
}
