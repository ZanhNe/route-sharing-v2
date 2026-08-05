package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "thong_bao", indexes = {
        @Index(name = "idx_thong_bao_nguoi_nhan_trang_thai", columnList = "nguoi_nhan_id,trang_thai_thong_bao"),
        @Index(name = "idx_thong_bao_doi_tuong", columnList = "loai_doi_tuong_lien_quan,doi_tuong_lien_quan_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThongBao extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao", nullable = false, length = 50)
    private LoaiThongBao loaiThongBao;
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;
    @Column(name = "noi_dung", nullable = false, length = 3000)
    private String noiDung;
    @Enumerated(EnumType.STRING)
    @Column(name = "kenh_gui", nullable = false, length = 20)
    private KenhThongBao kenhGui;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thong_bao", nullable = false, length = 20)
    private TrangThaiThongBao trangThaiThongBao = TrangThaiThongBao.PENDING;
    @Column(name = "gui_luc")
    private Instant guiLuc;
    @Column(name = "doc_luc")
    private Instant docLuc;
    @Column(name = "loai_doi_tuong_lien_quan", length = 100)
    private String loaiDoiTuongLienQuan;
    @Column(name = "doi_tuong_lien_quan_id")
    private Long doiTuongLienQuanId;
    @Column(name = "deduplication_key", length = 180)
    private String deduplicationKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_nhan_id", nullable = false)
    private NguoiDung nguoiNhan;

    public static ThongBao bookingRequest(
            YeuCauDiChung rideRequest,
            NguoiDung driver) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo thông báo.");
        }
        if (driver == null) {
            throw new IllegalArgumentException("Tài xế nhận thông báo không được trống.");
        }

        return ThongBao.builder()
                .loaiThongBao(LoaiThongBao.BOOKING_REQUEST)
                .tieuDe("Có yêu cầu đi chung mới")
                .noiDung("Một hành khách đã gửi yêu cầu tham gia lộ trình của bạn.")
                .kenhGui(KenhThongBao.IN_APP)
                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                .loaiDoiTuongLienQuan("YEU_CAU_DI_CHUNG")
                .doiTuongLienQuanId(rideRequest.getId())
                .deduplicationKey("BOOKING_REQUEST:" + rideRequest.getId() + ":CREATED")
                .nguoiNhan(driver)
                .build();
    }
}
