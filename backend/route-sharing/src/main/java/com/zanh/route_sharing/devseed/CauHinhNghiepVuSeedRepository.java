package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CauHinhNghiepVuSeedRepository
        extends JpaRepository<CauHinhNghiepVu, Long> {

    Optional<CauHinhNghiepVu> findByNhaTruong_Id(Long schoolId);
}
