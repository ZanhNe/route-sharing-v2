package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.NhomQuyen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NhomQuyenSeedRepository extends JpaRepository<NhomQuyen, Long> {
    @EntityGraph(attributePaths = "danhSachQuyenHan")
    Optional<NhomQuyen> findByMaNhomIgnoreCase(String maNhom);
}
