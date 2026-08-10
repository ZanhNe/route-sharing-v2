package com.zanh.route_sharing.security;

import com.zanh.route_sharing.config.properties.JwtProperties;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final String TOKEN_TYPE = "token_type";
    private static final String EMAIL = "email";
    private static final String ACCOUNT_STATUS = "account_status";
    private static final String SECURITY_VERSION = "security_version";
    private static final String AUTHORITIES = "authorities";

    private final JwtProperties properties;
    private final SecretKey key;
    private final JwtParser parser;
    private final Clock clock;

    public JwtUtil(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(properties.getBase64Secret());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("app.jwt.base64-secret phải là giá trị hợp lệ", exception);
        }
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT phải chứa ít nhất 32 byte");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.parser = Jwts.parser()
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .clock(() -> Date.from(TimePolicy.tokenNow(clock)))
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .verifyWith(key)
                .build();
    }

    public IssuedToken issueAccessToken(CustomUserDetails principal) {
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return issue(
                principal.getId(),
                principal.getUsername(),
                JwtTokenType.ACCESS,
                properties.getAccessTokenTtl(),
                builder -> builder
                        .claim(ACCOUNT_STATUS, principal.getTrangThaiTaiKhoan().name())
                        .claim(SECURITY_VERSION, principal.getSecurityVersion())
                        .claim(AUTHORITIES, authorities));
    }

    public IssuedToken issueRefreshToken(CustomUserDetails principal) {
        return issue(principal.getId(), principal.getUsername(), JwtTokenType.REFRESH,
                properties.getRefreshTokenTtl(), builder -> builder);
    }

    public JwtAccessClaims parseAccessToken(String token) {
        Claims claims = parse(token, JwtTokenType.ACCESS);
        TrangThaiTaiKhoan status;
        try {
            status = TrangThaiTaiKhoan.valueOf(requiredString(claims, ACCOUNT_STATUS));
        } catch (IllegalArgumentException exception) {
            throw new JwtException("account_status không hợp lệ", exception);
        }
        Number version = claims.get(SECURITY_VERSION, Number.class);
        if (version == null) {
            throw new JwtException("security_version không hợp lệ");
        }
        return new JwtAccessClaims(
                parseUserId(claims),
                requiredString(claims, EMAIL),
                status,
                version.longValue(),
                parseAuthorities(claims),
                requiredJwtId(claims),
                requiredExpiration(claims));
    }

    public JwtRefreshClaims parseRefreshToken(String token) {
        Claims claims = parse(token, JwtTokenType.REFRESH);
        return new JwtRefreshClaims(
                parseUserId(claims),
                requiredString(claims, EMAIL),
                requiredJwtId(claims),
                requiredExpiration(claims));
    }

    private IssuedToken issue(Long userId,
            String email,
            JwtTokenType type,
            Duration ttl,
            java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
        if (userId == null || email == null || email.isBlank()) {
            throw new IllegalArgumentException("JWT principal không hợp lệ");
        }
        Instant issuedAt = TimePolicy.tokenNow(clock);
        Instant expiresAt = issuedAt.plus(ttl);
        String jti = UUID.randomUUID().toString();

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .audience().add(properties.getAudience()).and()
                .id(jti)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(TOKEN_TYPE, type.name())
                .claim(EMAIL, email);

        String value = customizer.apply(builder).signWith(key).compact();
        return new IssuedToken(value, jti, expiresAt);
    }

    private Claims parse(String token, JwtTokenType expectedType) {
        if (token == null || token.isBlank()) {
            throw new JwtException("JWT không hợp lệ");
        }
        Claims claims = parser.parseSignedClaims(token).getPayload();
        String actualType = requiredString(claims, TOKEN_TYPE);
        if (!expectedType.name().equals(actualType)) {
            throw new JwtException("JWT không hợp lệ");
        }
        return claims;
    }

    private static Long parseUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (RuntimeException exception) {
            throw new JwtException("Invalid JWT subject", exception);
        }
    }

    private static String requiredString(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new JwtException("Không tìm thấy: " + name);
        }
        return value;
    }

    private static String requiredJwtId(Claims claims) {
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new JwtException("Không tìm thấy jti");
        }
        return claims.getId();
    }

    private static Instant requiredExpiration(Claims claims) {
        if (claims.getExpiration() == null) {
            throw new JwtException("Không tìm thấy exp");
        }
        return claims.getExpiration().toInstant();
    }

    private static List<String> parseAuthorities(Claims claims) {
        Object raw = claims.get(AUTHORITIES);
        if (!(raw instanceof List<?> list)) {
            throw new JwtException("Không tìm thấy authorities");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String authority) || authority.isBlank()) {
                throw new JwtException("Không tìm thấy authorities");
            }
            result.add(authority);
        }
        return List.copyOf(result);
    }
}
