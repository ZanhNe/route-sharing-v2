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
                        + "and tablename in ('yeu_cau_di_chung', 'thong_bao')",
                String.class);

        assertThat(columns).contains(
                "diem_tha_de_xuat",
                "muc_ho_tro_hanh_khach_de_nghi",
                "expires_at",
                "tai_xe_id_luc_gui",
                "cau_hinh_id_luc_gui",
                "cooldown_until");
        assertThat(indexes).contains(
                "uk_yeu_cau_hanh_khach_blocking",
                "idx_yeu_cau_cooldown_lookup",
                "idx_yeu_cau_route_queue",
                "uk_thong_bao_deduplication_key");
    }
}
