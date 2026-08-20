package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "phan_hoi_khieu_nai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_phan_hoi_khieu_nai_complaint", columnNames = "khieu_nai_id")
}, check = {
        @CheckConstraint(name = "ck_phan_hoi_khieu_nai_noi_dung", constraint = "char_length(btrim(noi_dung)) BETWEEN 20 AND 5000")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class PhanHoiKhieuNai extends Base {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khieu_nai_id", nullable = false, unique = true)
    private KhieuNai khieuNai;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_phan_hoi_id", nullable = false)
    private NguoiDung nguoiPhanHoi;

    @Column(name = "noi_dung", nullable = false, length = 5000)
    private String noiDung;

    @Column(name = "phan_hoi_luc", nullable = false)
    private Instant phanHoiLuc;

    public static PhanHoiKhieuNai submit(KhieuNai complaint, NguoiDung respondent, String normalizedContent,
            Instant submittedAt) {
        if (complaint == null || complaint.getId() == null || respondent == null || respondent.getId() == null
                || submittedAt == null) {
            throw new IllegalArgumentException("Complaint/respondent/submittedAt không hợp lệ.");
        }
        if (complaint.getNguoiBiKhieuNai() == null
                || !Objects.equals(complaint.getNguoiBiKhieuNai().getId(), respondent.getId())) {
            throw new IllegalArgumentException("Respondent không phải người bị khiếu nại của complaint.");
        }
        String content = normalizedContent == null ? null : normalizedContent.trim();
        if (content == null || content.length() < 20 || content.length() > 5000) {
            throw new IllegalArgumentException("Nội dung phản hồi phải từ 20 đến 5000 ký tự.");
        }
        PhanHoiKhieuNai response = new PhanHoiKhieuNai();
        response.khieuNai = complaint;
        response.nguoiPhanHoi = respondent;
        response.noiDung = content;
        response.phanHoiLuc = submittedAt;
        return response;
    }
}
