package com.zanh.route_sharing.config.database;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;

import java.util.Set;

public final class PostgreSqlSchemaContributor implements AdditionalMappingContributor {

    private static final Set<String> POSTGRES_DIALECTS = Set.of(
            "org.hibernate.dialect.PostgreSQLDialect");

    @Override
    public void contribute(
            AdditionalMappingContributions contributions,
            InFlightMetadataCollector metadata,
            ResourceStreamLocator resourceStreamLocator,
            MetadataBuildingContext buildingContext) {
        contributions.contributeAuxiliaryDatabaseObject(
                new SimpleAuxiliaryDatabaseObject(
                        POSTGRES_DIALECTS,
                        null,
                        null,
                        new String[] { "CREATE EXTENSION IF NOT EXISTS postgis" },
                        new String[0],
                        true));

        contributions.contributeAuxiliaryDatabaseObject(
                new SimpleAuxiliaryDatabaseObject(
                        POSTGRES_DIALECTS,
                        null,
                        null,
                        createAfterTables(),
                        dropBeforeTables(),
                        false));
    }

    static String[] createAfterTables() {
        return new String[] {

                "CREATE UNIQUE INDEX IF NOT EXISTS uk_yeu_cau_hanh_khach_blocking "
                        + "ON yeu_cau_di_chung (hanh_khach_id) "
                        + "WHERE trang_thai_yeu_cau IN "
                        + "('PENDING','ACCEPTED','ON_BOARD','DISPUTED')",
                "CREATE INDEX IF NOT EXISTS idx_yeu_cau_cooldown_lookup "
                        + "ON yeu_cau_di_chung "
                        + "(hanh_khach_id, lo_trinh_chia_se_id, cooldown_until DESC) "
                        + "WHERE trang_thai_yeu_cau = 'REJECTED' "
                        + "AND cooldown_until IS NOT NULL",
                "CREATE INDEX IF NOT EXISTS idx_yeu_cau_route_queue "
                        + "ON yeu_cau_di_chung "
                        + "(lo_trinh_chia_se_id, trang_thai_yeu_cau, gui_luc, id)",
                "CREATE INDEX IF NOT EXISTS idx_yeu_cau_passenger_history "
                        + "ON yeu_cau_di_chung "
                        + "(hanh_khach_id, gui_luc DESC, id DESC)",
                "CREATE INDEX IF NOT EXISTS idx_lo_trinh_driver_history "
                        + "ON lo_trinh_chia_se "
                        + "(tai_xe_id, created_at DESC, id DESC)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_thong_bao_deduplication_key "
                        + "ON thong_bao (deduplication_key) "
                        + "WHERE deduplication_key IS NOT NULL",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_start_moi_chuyen "
                        + "ON diem_dung_hanh_trinh (chuyen_di_id) "
                        + "WHERE loai_diem_dung = 'DRIVER_START'",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_end_moi_chuyen "
                        + "ON diem_dung_hanh_trinh (chuyen_di_id) "
                        + "WHERE loai_diem_dung = 'DRIVER_END'",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_ho_so_thanh_vien_hien_hanh "
                        + "ON ho_so_thanh_vien (nguoi_dung_id, nha_truong_id, loai_ho_so) "
                        + "WHERE trang_thai_ho_so IN "
                        + "('SUBMITTED','UNDER_REVIEW','NEED_SUPPLEMENT','APPROVED','SUSPENDED')",
                "CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user "
                        + "ON refresh_token_session (nguoi_dung_id) "
                        + "WHERE revoked_at IS NULL",
                "CREATE INDEX IF NOT EXISTS idx_lo_trinh_searchable "
                        + "ON lo_trinh_chia_se (thoi_gian_khoi_hanh_du_kien, id) "
                        + "WHERE trang_thai_lo_trinh = 'OPEN' AND so_ghe_con_lai > 0",

                // V1 — spatial indexes
                "CREATE INDEX IF NOT EXISTS gist_lo_trinh_diem_xuat_phat "
                        + "ON lo_trinh_chia_se USING GIST (diem_xuat_phat)",
                "CREATE INDEX IF NOT EXISTS gist_lo_trinh_diem_dich "
                        + "ON lo_trinh_chia_se USING GIST (diem_dich_tai_xe)",
                "CREATE INDEX IF NOT EXISTS gist_lo_trinh_tuyen_goc "
                        + "ON lo_trinh_chia_se USING GIST (tuyen_duong_goc)",
                "CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_don "
                        + "ON yeu_cau_di_chung USING GIST (diem_don_thuc_te)",
                "CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_dich "
                        + "ON yeu_cau_di_chung USING GIST (diem_dich_cuoi_cung_mong_muon)",
                "CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_tha "
                        + "ON yeu_cau_di_chung USING GIST (diem_tha_de_xuat)",
                "CREATE INDEX IF NOT EXISTS gist_yeu_cau_tuyen_mong_muon "
                        + "ON yeu_cau_di_chung USING GIST (tuyen_duong_mong_muon_hanh_khach)",
                "CREATE INDEX IF NOT EXISTS gist_yeu_cau_doan_phuc_vu "
                        + "ON yeu_cau_di_chung USING GIST (doan_tuyen_duoc_phuc_vu)",
                "CREATE INDEX IF NOT EXISTS gist_chuyen_di_tuyen_van_hanh "
                        + "ON chuyen_di USING GIST (tuyen_duong_van_hanh)",
                "CREATE INDEX IF NOT EXISTS gist_chuyen_di_vi_tri_cuoi "
                        + "ON chuyen_di USING GIST (vi_tri_cuoi_cung)",
                "CREATE INDEX IF NOT EXISTS gist_diem_dung_ke_hoach "
                        + "ON diem_dung_hanh_trinh USING GIST (toa_do_ke_hoach)",
                "CREATE INDEX IF NOT EXISTS gist_ban_ghi_dinh_vi "
                        + "ON ban_ghi_dinh_vi USING GIST (toa_do)",
                "CREATE INDEX IF NOT EXISTS gist_su_co_toa_do "
                        + "ON su_co_chuyen_di USING GIST (toa_do_xay_ra)",

                // UC-E1-02
                "CREATE INDEX IF NOT EXISTS gist_lo_trinh_tuyen_goc_geography "
                        + "ON lo_trinh_chia_se USING GIST ((tuyen_duong_goc::geography))",
                "CREATE INDEX IF NOT EXISTS gist_lo_trinh_diem_dich_geography "
                        + "ON lo_trinh_chia_se USING GIST ((diem_dich_tai_xe::geography))",

                "CREATE UNIQUE INDEX IF NOT EXISTS uk_nguoi_dung_email_truong_lower "
                        + "ON nguoi_dung (lower(email_truong))",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_nhom_quyen_ma_upper "
                        + "ON nhom_quyen (upper(ma_nhom))",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_quyen_han_ma_upper "
                        + "ON quyen_han (upper(ma_quyen))",

                securityVersionForDirectUserAssignmentsFunction(),
                "DROP TRIGGER IF EXISTS trg_bump_security_user_group ON nguoi_dung_nhom_quyen",
                "CREATE TRIGGER trg_bump_security_user_group "
                        + "AFTER INSERT OR UPDATE OR DELETE ON nguoi_dung_nhom_quyen "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_user_security_version()",
                "DROP TRIGGER IF EXISTS trg_bump_security_direct_permission ON nguoi_dung_quyen_truc_tiep",
                "CREATE TRIGGER trg_bump_security_direct_permission "
                        + "AFTER INSERT OR UPDATE OR DELETE ON nguoi_dung_quyen_truc_tiep "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_user_security_version()",

                securityVersionForGroupPermissionFunction(),
                "DROP TRIGGER IF EXISTS trg_bump_security_group_permission ON nhom_quyen_quyen_han",
                "CREATE TRIGGER trg_bump_security_group_permission "
                        + "AFTER INSERT OR UPDATE OR DELETE ON nhom_quyen_quyen_han "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_group_users()",

                securityVersionForGroupRecordFunction(),
                "DROP TRIGGER IF EXISTS trg_bump_security_group_record ON nhom_quyen",
                "CREATE TRIGGER trg_bump_security_group_record "
                        + "AFTER UPDATE OF ma_nhom, dang_hoat_dong ON nhom_quyen "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_users_for_group_record()",

                securityVersionForPermissionRecordFunction(),
                "DROP TRIGGER IF EXISTS trg_bump_security_permission_record ON quyen_han",
                "CREATE TRIGGER trg_bump_security_permission_record "
                        + "AFTER UPDATE OF ma_quyen, dang_hoat_dong ON quyen_han "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_users_for_permission_record()",

                securityVersionForAccountFieldsFunction(),
                "DROP TRIGGER IF EXISTS trg_bump_security_account_fields ON nguoi_dung",
                "CREATE TRIGGER trg_bump_security_account_fields "
                        + "BEFORE UPDATE OF trang_thai_tai_khoan, mat_khau_da_ma_hoa, email_truong ON nguoi_dung "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_user_security_fields_changed()",

                revokeRefreshTokensOnAccountChangeFunction(),
                "DROP TRIGGER IF EXISTS trg_revoke_refresh_account_change ON nguoi_dung",
                "CREATE TRIGGER trg_revoke_refresh_account_change "
                        + "AFTER UPDATE OF trang_thai_tai_khoan, mat_khau_da_ma_hoa, email_truong ON nguoi_dung "
                        + "FOR EACH ROW EXECUTE FUNCTION route_sharing_revoke_refresh_on_account_change()"
        };
    }

    static String[] dropBeforeTables() {
        return new String[] {

                "DROP FUNCTION IF EXISTS route_sharing_revoke_refresh_on_account_change() CASCADE",
                "DROP FUNCTION IF EXISTS route_sharing_user_security_fields_changed() CASCADE",
                "DROP FUNCTION IF EXISTS route_sharing_bump_users_for_permission_record() CASCADE",
                "DROP FUNCTION IF EXISTS route_sharing_bump_users_for_group_record() CASCADE",
                "DROP FUNCTION IF EXISTS route_sharing_bump_group_users() CASCADE",
                "DROP FUNCTION IF EXISTS route_sharing_bump_user_security_version() CASCADE",

                "DROP INDEX IF EXISTS uk_quyen_han_ma_upper",
                "DROP INDEX IF EXISTS uk_nhom_quyen_ma_upper",
                "DROP INDEX IF EXISTS uk_nguoi_dung_email_truong_lower",

                "DROP INDEX IF EXISTS idx_lo_trinh_searchable",
                "DROP INDEX IF EXISTS idx_refresh_token_active_user",
                "DROP INDEX IF EXISTS uk_ho_so_thanh_vien_hien_hanh",
                "DROP INDEX IF EXISTS uk_diem_dung_driver_end_moi_chuyen",
                "DROP INDEX IF EXISTS uk_diem_dung_driver_start_moi_chuyen",
                "DROP INDEX IF EXISTS uk_thong_bao_deduplication_key",
                "DROP INDEX IF EXISTS idx_lo_trinh_driver_history",
                "DROP INDEX IF EXISTS idx_yeu_cau_passenger_history",
                "DROP INDEX IF EXISTS idx_yeu_cau_route_queue",
                "DROP INDEX IF EXISTS idx_yeu_cau_cooldown_lookup",
                "DROP INDEX IF EXISTS uk_yeu_cau_hanh_khach_blocking",
                "DROP INDEX IF EXISTS uk_yeu_cau_hanh_khach_lo_trinh_active",

                "DROP INDEX IF EXISTS gist_lo_trinh_diem_dich_geography",
                "DROP INDEX IF EXISTS gist_lo_trinh_tuyen_goc_geography",
                "DROP INDEX IF EXISTS gist_su_co_toa_do",
                "DROP INDEX IF EXISTS gist_ban_ghi_dinh_vi",
                "DROP INDEX IF EXISTS gist_diem_dung_ke_hoach",
                "DROP INDEX IF EXISTS gist_chuyen_di_vi_tri_cuoi",
                "DROP INDEX IF EXISTS gist_chuyen_di_tuyen_van_hanh",
                "DROP INDEX IF EXISTS gist_yeu_cau_doan_phuc_vu",
                "DROP INDEX IF EXISTS gist_yeu_cau_tuyen_mong_muon",
                "DROP INDEX IF EXISTS gist_yeu_cau_diem_tha",
                "DROP INDEX IF EXISTS gist_yeu_cau_diem_dich",
                "DROP INDEX IF EXISTS gist_yeu_cau_diem_don",
                "DROP INDEX IF EXISTS gist_lo_trinh_tuyen_goc",
                "DROP INDEX IF EXISTS gist_lo_trinh_diem_dich",
                "DROP INDEX IF EXISTS gist_lo_trinh_diem_xuat_phat"
        };
    }

    private static String securityVersionForDirectUserAssignmentsFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_bump_user_security_version()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF TG_OP = 'DELETE' THEN
                        UPDATE nguoi_dung
                           SET security_version = security_version + 1
                         WHERE id = OLD.nguoi_dung_id;
                        RETURN OLD;
                    END IF;

                    UPDATE nguoi_dung
                       SET security_version = security_version + 1
                     WHERE id = NEW.nguoi_dung_id;

                    IF TG_OP = 'UPDATE'
                       AND OLD.nguoi_dung_id IS DISTINCT FROM NEW.nguoi_dung_id THEN
                        UPDATE nguoi_dung
                           SET security_version = security_version + 1
                         WHERE id = OLD.nguoi_dung_id;
                    END IF;

                    RETURN NEW;
                END $$
                """;
    }

    private static String securityVersionForGroupPermissionFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_bump_group_users()
                RETURNS trigger LANGUAGE plpgsql AS $$
                DECLARE
                    old_group_id bigint;
                    new_group_id bigint;
                BEGIN
                    old_group_id := CASE
                        WHEN TG_OP IN ('DELETE', 'UPDATE') THEN OLD.nhom_quyen_id
                        ELSE NULL
                    END;
                    new_group_id := CASE
                        WHEN TG_OP IN ('INSERT', 'UPDATE') THEN NEW.nhom_quyen_id
                        ELSE NULL
                    END;

                    IF old_group_id IS NOT NULL THEN
                        UPDATE nguoi_dung u
                           SET security_version = u.security_version + 1
                          FROM nguoi_dung_nhom_quyen ug
                         WHERE ug.nguoi_dung_id = u.id
                           AND ug.nhom_quyen_id = old_group_id;
                    END IF;

                    IF new_group_id IS NOT NULL
                       AND new_group_id IS DISTINCT FROM old_group_id THEN
                        UPDATE nguoi_dung u
                           SET security_version = u.security_version + 1
                          FROM nguoi_dung_nhom_quyen ug
                         WHERE ug.nguoi_dung_id = u.id
                           AND ug.nhom_quyen_id = new_group_id;
                    END IF;

                    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
                END $$
                """;
    }

    private static String securityVersionForGroupRecordFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_bump_users_for_group_record()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF OLD.ma_nhom IS DISTINCT FROM NEW.ma_nhom
                       OR OLD.dang_hoat_dong IS DISTINCT FROM NEW.dang_hoat_dong THEN
                        UPDATE nguoi_dung u
                           SET security_version = u.security_version + 1
                          FROM nguoi_dung_nhom_quyen ug
                         WHERE ug.nguoi_dung_id = u.id
                           AND ug.nhom_quyen_id = NEW.id;
                    END IF;
                    RETURN NEW;
                END $$
                """;
    }

    private static String securityVersionForPermissionRecordFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_bump_users_for_permission_record()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF OLD.ma_quyen IS DISTINCT FROM NEW.ma_quyen
                       OR OLD.dang_hoat_dong IS DISTINCT FROM NEW.dang_hoat_dong THEN
                        UPDATE nguoi_dung u
                           SET security_version = u.security_version + 1
                         WHERE EXISTS (
                                   SELECT 1
                                     FROM nguoi_dung_quyen_truc_tiep uq
                                    WHERE uq.nguoi_dung_id = u.id
                                      AND uq.quyen_han_id = NEW.id
                               )
                            OR EXISTS (
                                   SELECT 1
                                     FROM nguoi_dung_nhom_quyen ug
                                     JOIN nhom_quyen_quyen_han gq
                                       ON gq.nhom_quyen_id = ug.nhom_quyen_id
                                    WHERE ug.nguoi_dung_id = u.id
                                      AND gq.quyen_han_id = NEW.id
                               );
                    END IF;
                    RETURN NEW;
                END $$
                """;
    }

    private static String securityVersionForAccountFieldsFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_user_security_fields_changed()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF OLD.trang_thai_tai_khoan IS DISTINCT FROM NEW.trang_thai_tai_khoan
                       OR OLD.mat_khau_da_ma_hoa IS DISTINCT FROM NEW.mat_khau_da_ma_hoa
                       OR OLD.email_truong IS DISTINCT FROM NEW.email_truong THEN
                        NEW.security_version := OLD.security_version + 1;
                    END IF;
                    RETURN NEW;
                END $$
                """;
    }

    private static String revokeRefreshTokensOnAccountChangeFunction() {
        return """
                CREATE OR REPLACE FUNCTION route_sharing_revoke_refresh_on_account_change()
                RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF OLD.trang_thai_tai_khoan IS DISTINCT FROM NEW.trang_thai_tai_khoan
                       OR OLD.mat_khau_da_ma_hoa IS DISTINCT FROM NEW.mat_khau_da_ma_hoa
                       OR OLD.email_truong IS DISTINCT FROM NEW.email_truong THEN
                        UPDATE refresh_token_session
                           SET revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP)
                         WHERE nguoi_dung_id = NEW.id
                           AND revoked_at IS NULL;
                    END IF;
                    RETURN NEW;
                END $$
                """;
    }
}
