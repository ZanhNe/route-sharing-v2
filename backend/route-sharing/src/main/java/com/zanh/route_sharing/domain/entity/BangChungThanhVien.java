package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiBangChungThanhVien;
import com.zanh.route_sharing.domain.enums.ViTriBangChungThanhVien;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bang_chung_thanh_vien", indexes = {
        @Index(name = "idx_bang_chung_thanh_vien_ho_so", columnList = "ho_so_thanh_vien_id"),
        @Index(name = "idx_bang_chung_thanh_vien_hash", columnList = "sha256")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class BangChungThanhVien extends Base {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ho_so_thanh_vien_id", nullable = false)
    private HoSoThanhVien hoSoThanhVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 50)
    private ViTriBangChungThanhVien slot;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "verified_media_type", nullable = false, length = 120)
    private String verifiedMediaType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "storage_key", nullable = false, length = 100)
    private String storageKey;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_state", nullable = false, length = 30)
    private TrangThaiBangChungThanhVien reviewState = TrangThaiBangChungThanhVien.PENDING;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private boolean current = true;
}
