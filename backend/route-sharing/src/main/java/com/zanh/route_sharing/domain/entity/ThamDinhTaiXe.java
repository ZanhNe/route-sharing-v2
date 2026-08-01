package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tham_dinh_tai_xe")
@DiscriminatorValue("TAI_XE")
@PrimaryKeyJoinColumn(name = "lan_tham_dinh_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThamDinhTaiXe extends LanThamDinh {
    @Column(name = "anh_chan_dung_doi_chieu_url", length = 2048)
    private String anhChanDungDoiChieuUrl;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ho_so_tai_xe_id", nullable = false)
    private HoSoTaiXe hoSoTaiXe;
}
