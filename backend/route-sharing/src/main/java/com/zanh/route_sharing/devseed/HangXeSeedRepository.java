package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.HangXe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HangXeSeedRepository extends JpaRepository<HangXe, Long> {
    Optional<HangXe> findByMaHangIgnoreCase(String maHang);
}
