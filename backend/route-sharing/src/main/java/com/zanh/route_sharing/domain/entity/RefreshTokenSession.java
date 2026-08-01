package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "refresh_token_session", uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_jti", columnNames = "jti"), indexes = {
                @Index(name = "idx_refresh_token_user", columnList = "nguoi_dung_id"),
                @Index(name = "idx_refresh_token_expiry", columnList = "expires_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenSession extends Base {
        @Column(name = "jti", nullable = false, length = 64)
        private String jti;

        @Column(name = "token_hash", nullable = false, length = 64)
        private String tokenHash;

        @Column(name = "expires_at", nullable = false)
        private Instant expiresAt;

        @Column(name = "revoked_at")
        private Instant revokedAt;

        @Column(name = "replaced_by_jti", length = 64)
        private String replacedByJti;

        @Column(name = "ip_address", length = 64)
        private String ipAddress;

        @Column(name = "user_agent", length = 500)
        private String userAgent;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nguoi_dung_id", nullable = false)
        private NguoiDung nguoiDung;
}
