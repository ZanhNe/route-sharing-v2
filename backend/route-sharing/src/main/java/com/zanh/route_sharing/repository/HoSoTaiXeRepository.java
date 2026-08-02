package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HoSoTaiXeRepository extends JpaRepository<HoSoTaiXe, Long> {

    @EntityGraph(attributePaths = "nguoiDung")
    @Query("select h from HoSoTaiXe h where h.nguoiDung.id = :userId")
    Optional<HoSoTaiXe> findByUserIdForRouteCreation(@Param("userId") Long userId);
}
