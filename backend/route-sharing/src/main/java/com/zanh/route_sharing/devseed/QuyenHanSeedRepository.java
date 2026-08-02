package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.QuyenHan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuyenHanSeedRepository extends JpaRepository<QuyenHan, Long> {
    Optional<QuyenHan> findByMaQuyenIgnoreCase(String maQuyen);
}
