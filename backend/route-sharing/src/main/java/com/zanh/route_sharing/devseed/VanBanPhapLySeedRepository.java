package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.VanBanPhapLy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VanBanPhapLySeedRepository extends JpaRepository<VanBanPhapLy, Long> {
    Optional<VanBanPhapLy> findByNhaTruong_IdAndMaVanBanAndPhienBan(Long schoolId, String maVanBan, String phienBan);
}
