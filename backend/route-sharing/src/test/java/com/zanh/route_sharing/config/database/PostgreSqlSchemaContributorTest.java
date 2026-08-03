package com.zanh.route_sharing.config.database;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlSchemaContributorTest {

    @Test
    void createCommandsContainPostgisIndexesAndSecurityObjects() {
        String ddl = String.join("\n", PostgreSqlSchemaContributor.createAfterTables());

        assertThat(ddl)
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_yeu_cau_hanh_khach_lo_trinh_active")
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_start_moi_chuyen")
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_end_moi_chuyen")
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_ho_so_thanh_vien_hien_hanh")
                .contains("CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user")
                .contains("CREATE INDEX IF NOT EXISTS idx_lo_trinh_searchable")
                .contains("WHERE trang_thai_lo_trinh = 'OPEN' AND so_ghe_con_lai > 0")
                .contains("USING GIST (tuyen_duong_goc)")
                .contains("USING GIST ((tuyen_duong_goc::geography))")
                .contains("lower(email_truong)")
                .contains("upper(ma_nhom)")
                .contains("upper(ma_quyen)")
                .contains("route_sharing_bump_user_security_version")
                .contains("route_sharing_bump_group_users")
                .contains("route_sharing_user_security_fields_changed")
                .contains("route_sharing_revoke_refresh_on_account_change")
                .contains("trg_revoke_refresh_account_change");
    }

    @Test
    void schemaContributorDoesNotAlterEntityOwnedColumnsOrConstraints() {
        String ddl = String.join("\n", PostgreSqlSchemaContributor.createAfterTables());

        assertThat(ddl)
                .doesNotContain("ALTER TABLE")
                .doesNotContain("ADD COLUMN")
                .doesNotContain("ADD CONSTRAINT");
    }

    @Test
    void dropCommandsAreSafeAfterAnInterruptedSchemaCreation() {
        String ddl = String.join("\n", PostgreSqlSchemaContributor.dropBeforeTables());

        assertThat(ddl)
                .contains("DROP FUNCTION IF EXISTS route_sharing_bump_user_security_version() CASCADE")
                .contains("DROP FUNCTION IF EXISTS route_sharing_revoke_refresh_on_account_change() CASCADE")
                .contains("DROP INDEX IF EXISTS uk_yeu_cau_hanh_khach_lo_trinh_active")
                .contains("DROP INDEX IF EXISTS idx_lo_trinh_searchable")
                .contains("DROP INDEX IF EXISTS gist_lo_trinh_tuyen_goc")
                .doesNotContain("DROP TRIGGER");
    }

}
