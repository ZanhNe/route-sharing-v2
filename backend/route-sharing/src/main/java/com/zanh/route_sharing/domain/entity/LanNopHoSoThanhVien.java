package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "lan_nop_ho_so_thanh_vien", uniqueConstraints = {
                @UniqueConstraint(name = "uk_lan_nop_ho_so_thanh_vien", columnNames = { "ho_so_thanh_vien_id",
                                "lan_nop" })
}, indexes = {
                @Index(name = "idx_lan_nop_ho_so_thanh_vien_ho_so", columnList = "ho_so_thanh_vien_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class LanNopHoSoThanhVien extends Base {
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "ho_so_thanh_vien_id", nullable = false)
        private HoSoThanhVien hoSoThanhVien;

        @Column(name = "lan_nop", nullable = false)
        private int lanNop;

        @Column(name = "nop_luc", nullable = false)
        private Instant nopLuc;

        @Column(name = "policy_key", nullable = false, length = 100)
        private String policyKey;

        @Column(name = "policy_version", nullable = false)
        private int policyVersion;

        @Column(name = "student_code_snapshot", nullable = false, length = 100)
        private String studentCodeSnapshot;

        @Column(name = "currently_studying_snapshot", nullable = false)
        private boolean currentlyStudyingSnapshot;

        @Column(name = "ngay_nhap_hoc_snapshot")
        private LocalDate ngayNhapHocSnapshot;

        @Column(name = "lop_id_snapshot")
        private Long lopIdSnapshot;

        @Column(name = "school_id_snapshot", nullable = false)
        private Long schoolIdSnapshot;

        @Builder.Default
        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "lan_nop_ho_so_thanh_vien_bang_chung", joinColumns = @JoinColumn(name = "submission_id"), inverseJoinColumns = @JoinColumn(name = "evidence_id"), uniqueConstraints = @UniqueConstraint(name = "uk_lan_nop_ho_so_bang_chung", columnNames = {
                        "submission_id", "evidence_id" }))
        private Set<BangChungThanhVien> bangChungDaNop = new LinkedHashSet<>();
}
