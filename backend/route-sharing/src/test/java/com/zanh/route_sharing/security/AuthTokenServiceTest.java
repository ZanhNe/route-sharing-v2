package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.entity.RefreshTokenSession;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.RefreshTokenSessionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {
    @Test
    void reusedRotatedRefreshTokenRevokesTheWholeTokenFamilyInFollowupTransaction() {
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        RefreshTokenSessionRepository repository = mock(RefreshTokenSessionRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        String rawToken = "signed-refresh-token";
        JwtRefreshClaims claims = new JwtRefreshClaims(
                9L,
                "user@school.edu.vn",
                "old-jti",
                now.plusSeconds(600)
        );
        RefreshTokenSession alreadyRotated = RefreshTokenSession.builder()
                .jti("old-jti")
                .tokenHash(RefreshTokenHasher.sha256(rawToken))
                .expiresAt(now.plusSeconds(600))
                .revokedAt(now.minusSeconds(5))
                .replacedByJti("new-jti")
                .build();

        when(jwtUtil.parseRefreshToken(rawToken)).thenReturn(claims);
        when(repository.findForUpdateByJti("old-jti")).thenReturn(Optional.of(alreadyRotated));

        AuthTokenService service = new AuthTokenService(
                jwtUtil,
                userDetailsService,
                repository,
                entityManager,
                clock,
                transactionTemplate
        );

        assertThatThrownBy(() -> service.rotate(rawToken, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));

        verify(repository).revokeAllActiveByUserId(9L, now);
        assertThat(transactionManager.rollbacks).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(1);
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {
        private int commits;
        private int rollbacks;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits++;
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbacks++;
        }
    }
}
