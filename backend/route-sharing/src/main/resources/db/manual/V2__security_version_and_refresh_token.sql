CREATE UNIQUE INDEX IF NOT EXISTS uk_nguoi_dung_email_truong_lower
    ON nguoi_dung (lower(email_truong));

CREATE UNIQUE INDEX IF NOT EXISTS uk_nhom_quyen_ma_upper
    ON nhom_quyen (upper(ma_nhom));

CREATE UNIQUE INDEX IF NOT EXISTS uk_quyen_han_ma_upper
    ON quyen_han (upper(ma_quyen));

CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user
    ON refresh_token_session (nguoi_dung_id)
    WHERE revoked_at IS NULL;

ALTER TABLE nguoi_dung
    ADD COLUMN IF NOT EXISTS security_version bigint;

UPDATE nguoi_dung
   SET security_version = 0
 WHERE security_version IS NULL;

ALTER TABLE nguoi_dung
    ALTER COLUMN security_version SET DEFAULT 0,
    ALTER COLUMN security_version SET NOT NULL;

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

    IF TG_OP = 'UPDATE' AND OLD.nguoi_dung_id IS DISTINCT FROM NEW.nguoi_dung_id THEN
        UPDATE nguoi_dung
           SET security_version = security_version + 1
         WHERE id = OLD.nguoi_dung_id;
    END IF;

    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_bump_security_user_group ON nguoi_dung_nhom_quyen;
CREATE TRIGGER trg_bump_security_user_group
AFTER INSERT OR UPDATE OR DELETE ON nguoi_dung_nhom_quyen
FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_user_security_version();

DROP TRIGGER IF EXISTS trg_bump_security_direct_permission ON nguoi_dung_quyen_truc_tiep;
CREATE TRIGGER trg_bump_security_direct_permission
AFTER INSERT OR UPDATE OR DELETE ON nguoi_dung_quyen_truc_tiep
FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_user_security_version();

CREATE OR REPLACE FUNCTION route_sharing_bump_group_users()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    old_group_id bigint;
    new_group_id bigint;
BEGIN
    old_group_id := CASE WHEN TG_OP IN ('DELETE', 'UPDATE') THEN OLD.nhom_quyen_id ELSE NULL END;
    new_group_id := CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN NEW.nhom_quyen_id ELSE NULL END;

    IF old_group_id IS NOT NULL THEN
        UPDATE nguoi_dung u
           SET security_version = u.security_version + 1
          FROM nguoi_dung_nhom_quyen ug
         WHERE ug.nguoi_dung_id = u.id
           AND ug.nhom_quyen_id = old_group_id;
    END IF;

    IF new_group_id IS NOT NULL AND new_group_id IS DISTINCT FROM old_group_id THEN
        UPDATE nguoi_dung u
           SET security_version = u.security_version + 1
          FROM nguoi_dung_nhom_quyen ug
         WHERE ug.nguoi_dung_id = u.id
           AND ug.nhom_quyen_id = new_group_id;
    END IF;

    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

DROP TRIGGER IF EXISTS trg_bump_security_group_permission ON nhom_quyen_quyen_han;
CREATE TRIGGER trg_bump_security_group_permission
AFTER INSERT OR UPDATE OR DELETE ON nhom_quyen_quyen_han
FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_group_users();

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
END $$;

DROP TRIGGER IF EXISTS trg_bump_security_group_record ON nhom_quyen;
CREATE TRIGGER trg_bump_security_group_record
AFTER UPDATE OF ma_nhom, dang_hoat_dong ON nhom_quyen
FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_users_for_group_record();

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
END $$;

DROP TRIGGER IF EXISTS trg_bump_security_permission_record ON quyen_han;
CREATE TRIGGER trg_bump_security_permission_record
AFTER UPDATE OF ma_quyen, dang_hoat_dong ON quyen_han
FOR EACH ROW EXECUTE FUNCTION route_sharing_bump_users_for_permission_record();

CREATE OR REPLACE FUNCTION route_sharing_user_security_fields_changed()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.trang_thai_tai_khoan IS DISTINCT FROM NEW.trang_thai_tai_khoan
       OR OLD.mat_khau_da_ma_hoa IS DISTINCT FROM NEW.mat_khau_da_ma_hoa
       OR OLD.email_truong IS DISTINCT FROM NEW.email_truong THEN
        NEW.security_version := OLD.security_version + 1;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_bump_security_account_fields ON nguoi_dung;
CREATE TRIGGER trg_bump_security_account_fields
BEFORE UPDATE OF trang_thai_tai_khoan, mat_khau_da_ma_hoa, email_truong ON nguoi_dung
FOR EACH ROW EXECUTE FUNCTION route_sharing_user_security_fields_changed();

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
END $$;

DROP TRIGGER IF EXISTS trg_revoke_refresh_account_change ON nguoi_dung;
CREATE TRIGGER trg_revoke_refresh_account_change
AFTER UPDATE OF trang_thai_tai_khoan, mat_khau_da_ma_hoa, email_truong ON nguoi_dung
FOR EACH ROW EXECUTE FUNCTION route_sharing_revoke_refresh_on_account_change();
