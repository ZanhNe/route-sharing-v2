package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "don_vi_cong_tac")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DonViCongTac extends Base {

    @Column(nullable = false)
    private String tenDonVi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_chuc_id", nullable = false)
    private ToChuc toChuc;
}
