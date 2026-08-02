package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmailTruongIgnoreCase(String emailTruong);
}
