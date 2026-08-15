package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.HoSoNhanSu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoSoNhanSuSeedRepository extends JpaRepository<HoSoNhanSu, Long> {
    Optional<HoSoNhanSu> findFirstByNguoiDung_IdAndNhaTruong_Id(Long userId, Long schoolId);
}
