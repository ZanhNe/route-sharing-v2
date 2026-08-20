package com.zanh.route_sharing.repository.iam.emailverification;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NguoiDungEmailVerificationRepository extends JpaRepository<NguoiDung, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from NguoiDung u where u.id = :id")
    Optional<NguoiDung> findByIdForUpdate(@Param("id") Long id);
}
