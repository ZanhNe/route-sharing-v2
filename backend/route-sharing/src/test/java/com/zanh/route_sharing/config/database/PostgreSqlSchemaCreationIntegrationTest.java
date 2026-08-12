package com.zanh.route_sharing.config.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgreSqlSchemaCreationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void givenCleanSchema_whenApplicationStarts_thenRideRequestColumnsAndIndexesExist() {
        List<String> columns = jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'yeu_cau_di_chung'",
                String.class);
        List<String> indexes = jdbc.queryForList(
                "select indexname from pg_indexes "
                        + "where schemaname = current_schema() "
                        + "and tablename in ('yeu_cau_di_chung', 'thong_bao', 'lo_trinh_chia_se')",
                String.class);

        assertThat(columns).contains(
                "diem_tha_de_xuat",
                "muc_ho_tro_hanh_khach_de_nghi",
                "huy_luc",
                "ly_do_huy",
                "tai_xe_id_luc_gui",
                "cau_hinh_id_luc_gui",
                "cooldown_until",
                "khong_den_luc");
        assertThat(columns).doesNotContain("expires_at", "request_ttl_applied_seconds");
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'nhat_ky_trang_thai_lo_trinh'",
                Long.class)).isEqualTo(1L);
        assertThat(indexes).contains(
                "uk_yeu_cau_hanh_khach_blocking",
                "idx_yeu_cau_cooldown_lookup",
                "idx_yeu_cau_route_queue",
                "idx_yeu_cau_passenger_history",
                "idx_lo_trinh_driver_history",
                "uk_thong_bao_deduplication_key");
        String cooldownIndexDefinition = jdbc.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = current_schema() "
                        + "and tablename = 'yeu_cau_di_chung' "
                        + "and indexname = 'idx_yeu_cau_cooldown_lookup'",
                String.class);
        assertThat(cooldownIndexDefinition)
                .contains("hanh_khach_id", "lo_trinh_chia_se_id", "cooldown_until")
                .doesNotContain("tai_xe_id_luc_gui");

        String passengerHistoryIndexDefinition = jdbc.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = current_schema() "
                        + "and tablename = 'yeu_cau_di_chung' "
                        + "and indexname = 'idx_yeu_cau_passenger_history'",
                String.class);
        assertThat(passengerHistoryIndexDefinition)
                .contains("hanh_khach_id", "gui_luc DESC", "id DESC");

        String driverHistoryIndexDefinition = jdbc.queryForObject(
                "select indexdef from pg_indexes "
                        + "where schemaname = current_schema() "
                        + "and tablename = 'lo_trinh_chia_se' "
                        + "and indexname = 'idx_lo_trinh_driver_history'",
                String.class);
        assertThat(driverHistoryIndexDefinition)
                .contains("tai_xe_id", "created_at DESC", "id DESC");
        List<String> boardingCredentialColumns = jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'thong_tin_xac_thuc_len_xe'",
                String.class);
        assertThat(boardingCredentialColumns).contains(
                "chuyen_di_id",
                "yeu_cau_di_chung_id",
                "diem_dung_hanh_trinh_id",
                "ma_ma_hoa",
                "nonce_ma_hoa",
                "phien_ban_khoa",
                "kich_hoat_luc",
                "vo_hieu_hoa_luc");
        assertThat(boardingCredentialColumns).doesNotContain("boarding_code", "expires_at");
        assertThat(jdbc.queryForObject(
                "select count(*) "
                        + "from information_schema.table_constraints tc "
                        + "join information_schema.key_column_usage kcu "
                        + "on tc.constraint_catalog = kcu.constraint_catalog "
                        + "and tc.constraint_schema = kcu.constraint_schema "
                        + "and tc.constraint_name = kcu.constraint_name "
                        + "where tc.constraint_schema = current_schema() "
                        + "and tc.table_name = 'thong_tin_xac_thuc_len_xe' "
                        + "and tc.constraint_type = 'UNIQUE' "
                        + "and kcu.column_name = 'diem_dung_hanh_trinh_id' "
                        + "and (select count(*) from information_schema.key_column_usage k2 "
                        + "where k2.constraint_catalog = tc.constraint_catalog "
                        + "and k2.constraint_schema = tc.constraint_schema "
                        + "and k2.constraint_name = tc.constraint_name) = 1",
                Long.class)).isEqualTo(1L);

    }
}
