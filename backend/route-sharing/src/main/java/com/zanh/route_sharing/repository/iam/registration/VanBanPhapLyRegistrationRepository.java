package com.zanh.route_sharing.repository.iam.registration;

import com.zanh.route_sharing.domain.entity.VanBanPhapLy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VanBanPhapLyRegistrationRepository extends JpaRepository<VanBanPhapLy, Long> {

  @Query("""
      select v
        from VanBanPhapLy v
       where v.nhaTruong.id = :schoolId
         and v.hieuLucTu <= :now
         and (v.hieuLucDen is null or :now < v.hieuLucDen)
       order by v.batBuoc desc, v.loaiVanBan asc, v.maVanBan asc, v.id asc
      """)
  List<VanBanPhapLy> findCurrentEffectiveForSchool(@Param("schoolId") Long schoolId,
      @Param("now") Instant now);
}
