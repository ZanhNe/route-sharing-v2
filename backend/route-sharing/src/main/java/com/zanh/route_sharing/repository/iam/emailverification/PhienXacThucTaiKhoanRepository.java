package com.zanh.route_sharing.repository.iam.emailverification;

import com.zanh.route_sharing.domain.entity.PhienXacThucTaiKhoan;
import com.zanh.route_sharing.domain.enums.MucDichXacThucTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiPhienXacThucTaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PhienXacThucTaiKhoanRepository extends JpaRepository<PhienXacThucTaiKhoan, Long> {
        Optional<PhienXacThucTaiKhoan> findFirstByNguoiDungIdAndMucDichOrderByCreatedAtDesc(
                        Long nguoiDungId,
                        MucDichXacThucTaiKhoan mucDich);

        Optional<PhienXacThucTaiKhoan> findFirstByNguoiDungIdAndMucDichAndTrangThaiOrderByCreatedAtDesc(
                        Long nguoiDungId,
                        MucDichXacThucTaiKhoan mucDich,
                        TrangThaiPhienXacThucTaiKhoan trangThai);

        List<PhienXacThucTaiKhoan> findByNguoiDungIdAndMucDichAndTrangThaiIn(
                        Long nguoiDungId,
                        MucDichXacThucTaiKhoan mucDich,
                        Collection<TrangThaiPhienXacThucTaiKhoan> trangThai);

        Optional<PhienXacThucTaiKhoan> findByIdAndNguoiDungId(Long id, Long nguoiDungId);

        @Modifying
        @Query("""
                        delete from PhienXacThucTaiKhoan p
                         where p.mucDich = :purpose
                           and p.trangThai in :terminalStates
                           and p.updatedAt < :cutoff
                        """)
        int deleteTerminalBefore(
                        @Param("purpose") MucDichXacThucTaiKhoan purpose,
                        @Param("terminalStates") Collection<TrangThaiPhienXacThucTaiKhoan> terminalStates,
                        @Param("cutoff") Instant cutoff);
}
