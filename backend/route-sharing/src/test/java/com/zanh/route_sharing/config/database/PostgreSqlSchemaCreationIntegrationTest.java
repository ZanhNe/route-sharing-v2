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
                "cooldown_until");
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
    }
}
