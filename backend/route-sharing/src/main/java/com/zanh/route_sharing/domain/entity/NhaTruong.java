package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "nha_truong", uniqueConstraints = {
                @UniqueConstraint(name = "uk_nha_truong_ma", columnNames = "ma_truong")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NhaTruong extends Base {
        @Column(name = "ma_truong", nullable = false, length = 50)
        private String maTruong;
        @Column(name = "ten_truong", nullable = false, length = 255)
        private String tenTruong;
        @Column(name = "ten_viet_tat", length = 50)
        private String tenVietTat;
        @Column(name = "logo_url", length = 2048)
        private String logoUrl;
        @Column(name = "dia_chi", nullable = false, length = 500)
        private String diaChi;
        @Builder.Default
        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(name = "nha_truong_ten_mien_email", joinColumns = @JoinColumn(name = "nha_truong_id"), uniqueConstraints = @UniqueConstraint(name = "uk_nha_truong_ten_mien", columnNames = {
                        "nha_truong_id", "ten_mien" }))
        @Column(name = "ten_mien", nullable = false, length = 255)
        private Set<String> tenMienEmailChoPhep = new LinkedHashSet<>();
        @Builder.Default
        @Column(name = "dang_hoat_dong", nullable = false)
        private Boolean dangHoatDong = true;
}
