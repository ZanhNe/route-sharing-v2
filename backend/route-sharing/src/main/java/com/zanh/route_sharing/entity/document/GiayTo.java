package com.zanh.route_sharing.entity.document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zanh.route_sharing.entity.Base;
import com.zanh.route_sharing.entity.NguoiDung;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiGiayTo;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "giay_to")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "loai_giay_to")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class GiayTo extends Base {

    @Column(nullable = false)
    private String soGiayTo;

    @Column(nullable = false)
    private LocalDate ngayCap;

    private LocalDate ngayHetHan;

    @Column(nullable = false)
    private String coQuanCap;

    @Column(nullable = false)
    private String matTruocUrl;
    private String matSauUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiGiayTo trangThai;

    private String lyDoTuChoi;
    private LocalDateTime ngayDuyet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

}