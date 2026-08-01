package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NguoiDungSecurityRepository extends JpaRepository<NguoiDung, Long> {

    @EntityGraph(attributePaths = {
            "danhSachNhomQuyen",
            "danhSachNhomQuyen.danhSachQuyenHan",
            "danhSachQuyenTrucTiep"
    })
    Optional<NguoiDung> findByEmailTruongIgnoreCase(String emailTruong);

    @EntityGraph(attributePaths = {
            "danhSachNhomQuyen",
            "danhSachNhomQuyen.danhSachQuyenHan",
            "danhSachQuyenTrucTiep"
    })
    @Query("select u from NguoiDung u where u.id = :id")
    Optional<NguoiDung> findPrincipalById(@Param("id") Long id);

    @Query("""
            select u.trangThaiTaiKhoan as trangThaiTaiKhoan,
                   u.securityVersion as securityVersion
            from NguoiDung u
            where u.id = :id
            """)
    Optional<SecurityStateProjection> findSecurityStateById(@Param("id") Long id);
}
