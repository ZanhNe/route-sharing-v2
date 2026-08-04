package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoTrinhChiaSeSeedRepository
        extends JpaRepository<LoTrinhChiaSe, Long> {

    Optional<LoTrinhChiaSe> findFirstByTaiXe_IdAndDiaChiXuatPhat(
            Long driverId,
            String originAddress);
}
