package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EntitySchemaMetadataTest {

    @Test
    void entitiesContainAllV1CheckConstraints() {
        Map<Class<?>, Set<String>> expected = Map.ofEntries(
                Map.entry(CauHinhNghiepVu.class, Set.of(
                        "ck_cau_hinh_ty_le_tien_duong",
                        "ck_cau_hinh_gia_tri_duong"
                )),
                Map.entry(PhuongTien.class, Set.of(
                        "ck_phuong_tien_so_cho",
                        "ck_phuong_tien_co_so_su_dung"
                )),
                Map.entry(LoTrinhChiaSe.class, Set.of(
                        "ck_lo_trinh_so_ghe",
                        "ck_lo_trinh_khoang_cach",
                        "ck_lo_trinh_muc_ho_tro"
                )),
                Map.entry(YeuCauDiChung.class, Set.of(
                        "ck_yeu_cau_ty_le",
                        "ck_yeu_cau_khoang_cach",
                        "ck_yeu_cau_matching",
                        "ck_yeu_cau_muc_ho_tro"
                )),
                Map.entry(ChuyenDi.class, Set.of("ck_chuyen_di_so_khach")),
                Map.entry(DiemDungHanhTrinh.class, Set.of(
                        "ck_diem_dung_thu_tu",
                        "ck_diem_dung_ban_kinh",
                        "ck_diem_dung_booking"
                )),
                Map.entry(BanGhiDinhVi.class, Set.of(
                        "ck_ban_ghi_thu_tu",
                        "ck_ban_ghi_do_chinh_xac",
                        "ck_ban_ghi_toc_do",
                        "ck_ban_ghi_huong"
                )),
                Map.entry(TepMinhChung.class, Set.of("ck_tep_minh_chung_xor")),
                Map.entry(QuyetDinhKyLuat.class, Set.of(
                        "ck_quyet_dinh_can_cu",
                        "ck_quyet_dinh_hieu_luc"
                )),
                Map.entry(DanhGia.class, Set.of(
                        "ck_danh_gia_so_sao",
                        "ck_danh_gia_hai_nguoi"
                )),
                Map.entry(BienLai.class, Set.of("ck_bien_lai_gia_tri")),
                Map.entry(PhienXacThucTaiKhoan.class, Set.of(
                        "ck_phien_xac_thuc_lan_thu",
                        "ck_phien_xac_thuc_han"
                )),
                Map.entry(LanThamDinh.class, Set.of("ck_lan_tham_dinh_lan_nop")),
                Map.entry(NhatKyPhanQuyen.class, Set.of("ck_nhat_ky_phan_quyen_du_lieu"))
        );

        expected.forEach((entityType, expectedNames) -> {
            Table table = entityType.getAnnotation(Table.class);
            assertThat(table)
                    .as("@Table của %s", entityType.getSimpleName())
                    .isNotNull();

            Set<String> actualNames = Arrays.stream(table.check())
                    .map(CheckConstraint::name)
                    .collect(Collectors.toSet());

            assertThat(actualNames)
                    .as("check constraints của %s", entityType.getSimpleName())
                    .containsExactlyInAnyOrderElementsOf(expectedNames);
        });
    }

    @Test
    void entityAnnotationsDoNotRenderPostgresqlPartialIndexesInsideCreateTable() {
        Set<Class<?>> entityTypes = Set.of(
                YeuCauDiChung.class,
                DiemDungHanhTrinh.class,
                HoSoThanhVien.class,
                RefreshTokenSession.class,
                LoTrinhChiaSe.class
        );

        entityTypes.forEach(entityType -> {
            Table table = entityType.getAnnotation(Table.class);

            assertThat(Arrays.stream(table.indexes())
                    .map(jakarta.persistence.Index::options)
                    .filter(options -> options != null && !options.isBlank())
                    .toList())
                    .as("PostgreSQL partial index options trên %s", entityType.getSimpleName())
                    .isEmpty();
        });
    }

    @Test
    void matchingDestinationRadiusIsOwnedByBusinessConfigurationEntity() throws Exception {
        Field field = CauHinhNghiepVu.class
                .getDeclaredField("banKinhDiemDenGanTuyenMet");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("ban_kinh_diem_den_gan_tuyen_met");
        assertThat(column.nullable()).isFalse();
        assertThat(column.precision()).isEqualTo(12);
        assertThat(column.scale()).isEqualTo(2);
    }

}
