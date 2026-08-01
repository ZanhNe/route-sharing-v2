package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenSession r join fetch r.nguoiDung where r.jti = :jti")
    Optional<RefreshTokenSession> findForUpdateByJti(@Param("jti") String jti);

    @Modifying
    @Query("""
            update RefreshTokenSession r
               set r.revokedAt = :revokedAt
             where r.nguoiDung.id = :userId
               and r.revokedAt is null
            """)
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("delete from RefreshTokenSession r where r.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);
}
