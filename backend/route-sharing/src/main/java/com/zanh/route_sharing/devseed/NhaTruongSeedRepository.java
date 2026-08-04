package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.NhaTruong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NhaTruongSeedRepository extends JpaRepository<NhaTruong, Long> {
    Optional<NhaTruong> findByMaTruongIgnoreCase(String maTruong);
}
