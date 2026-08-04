package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.HoSoSinhVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoSoSinhVienSeedRepository
                extends JpaRepository<HoSoSinhVien, Long> {

        Optional<HoSoSinhVien> findFirstByNguoiDung_IdAndNhaTruong_Id(
                        Long userId,
                        Long schoolId);
}
