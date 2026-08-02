package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.entity.PhuongTien;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PhuongTienRepository extends JpaRepository<PhuongTien, Long> {

    @EntityGraph(attributePaths = { "nguoiDangKySuDung", "dongXe", "dongXe.hangXe" })
    @Query("select p from PhuongTien p where p.id = :vehicleId")
    Optional<PhuongTien> findByIdForRouteCreation(@Param("vehicleId") Long vehicleId);

    Optional<PhuongTien> findByBienSoXeIgnoreCase(String bienSoXe);
}
