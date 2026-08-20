package com.zanh.route_sharing.repository.iam.registration;

import com.zanh.route_sharing.domain.entity.NhaTruong;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NhaTruongRegistrationRepository extends JpaRepository<NhaTruong, Long> {

    @EntityGraph(attributePaths = "tenMienEmailChoPhep")
    @Query("select distinct n from NhaTruong n where n.dangHoatDong = true order by n.tenTruong asc, n.id asc")
    List<NhaTruong> findRegistrationCandidates();

    @EntityGraph(attributePaths = "tenMienEmailChoPhep")
    @Query("select distinct n from NhaTruong n where n.id = :id and n.dangHoatDong = true")
    Optional<NhaTruong> findActiveForRegistrationRead(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select n from NhaTruong n where n.id = :id and n.dangHoatDong = true")
    Optional<NhaTruong> findActiveForRegistrationLock(@Param("id") Long id);
}
