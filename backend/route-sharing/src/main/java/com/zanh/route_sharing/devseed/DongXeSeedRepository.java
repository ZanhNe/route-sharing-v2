package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.DongXe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DongXeSeedRepository extends JpaRepository<DongXe, Long> {
    Optional<DongXe> findByHangXe_IdAndTenDongXeIgnoreCase(Long hangXeId, String tenDongXe);
}
