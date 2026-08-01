CREATE EXTENSION IF NOT EXISTS postgis;

CREATE OR REPLACE FUNCTION route_sharing_add_constraint(
    p_table regclass,
    p_name text,
    p_definition text
) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = p_table
           AND conname = p_name
    ) THEN
        EXECUTE format(
            'ALTER TABLE %s ADD CONSTRAINT %I %s',
            p_table,
            p_name,
            p_definition
        );
    END IF;
END $$;

SELECT route_sharing_add_constraint(
    'cau_hinh_nghiep_vu',
    'ck_cau_hinh_ty_le_tien_duong',
    'CHECK (ty_le_tien_duong_toi_thieu BETWEEN 0 AND 100)'
);
SELECT route_sharing_add_constraint(
    'cau_hinh_nghiep_vu',
    'ck_cau_hinh_gia_tri_duong',
    'CHECK (
        ban_kinh_cung_diem_den_met > 0 AND
        khoang_cach_lech_don_toi_da_met >= 0 AND
        thoi_gian_lech_don_toi_da_giay >= 0 AND
        ban_kinh_xac_dinh_da_den_met > 0 AND
        thoi_gian_cho_khach_giay >= 0 AND
        thoi_gian_mat_tin_hieu_giay > 0
    )'
);

SELECT route_sharing_add_constraint(
    'phuong_tien',
    'ck_phuong_tien_so_cho',
    'CHECK (so_cho_hanh_khach_duoc_duyet > 0)'
);
SELECT route_sharing_add_constraint(
    'phuong_tien',
    'ck_phuong_tien_co_so_su_dung',
    'CHECK (
        co_so_su_dung <> ''DUOC_CHU_XE_CHO_PHEP''
        OR (da_cam_ket_duoc_chu_xe_cho_phep = TRUE AND cam_ket_luc IS NOT NULL)
    )'
);

SELECT route_sharing_add_constraint(
    'lo_trinh_chia_se',
    'ck_lo_trinh_so_ghe',
    'CHECK (
        so_ghe_cung_cap > 0 AND
        so_ghe_con_lai >= 0 AND
        so_ghe_con_lai <= so_ghe_cung_cap
    )'
);
SELECT route_sharing_add_constraint(
    'lo_trinh_chia_se',
    'ck_lo_trinh_khoang_cach',
    'CHECK (khoang_cach_du_kien_met >= 0 AND thoi_luong_du_kien_giay >= 0)'
);
SELECT route_sharing_add_constraint(
    'lo_trinh_chia_se',
    'ck_lo_trinh_muc_ho_tro',
    'CHECK (muc_ho_tro_goi_y_moi_km IS NULL OR muc_ho_tro_goi_y_moi_km >= 0)'
);

SELECT route_sharing_add_constraint(
    'yeu_cau_di_chung',
    'ck_yeu_cau_ty_le',
    'CHECK (ty_le_tien_duong BETWEEN 0 AND 100)'
);
SELECT route_sharing_add_constraint(
    'yeu_cau_di_chung',
    'ck_yeu_cau_khoang_cach',
    'CHECK (
        khoang_cach_lech_de_don_met >= 0 AND
        thoi_gian_lech_de_don_giay >= 0 AND
        tong_khoang_cach_mong_muon_met > 0 AND
        khoang_cach_duoc_phuc_vu_met >= 0 AND
        khoang_cach_con_lai_met >= 0
    )'
);
SELECT route_sharing_add_constraint(
    'yeu_cau_di_chung',
    'ck_yeu_cau_matching',
    'CHECK (
        (loai_ghep_tuyen = ''CUNG_DIEM_DEN''
            AND loai_diem_tha = ''DIEM_DICH_CUOI_CUNG''
            AND khoang_cach_con_lai_met = 0)
        OR
        (loai_ghep_tuyen = ''TRUNG_DOAN_TUYEN''
            AND loai_diem_tha = ''DIEM_THA_TRUNG_GIAN''
            AND khoang_cach_con_lai_met > 0)
    )'
);
SELECT route_sharing_add_constraint(
    'yeu_cau_di_chung',
    'ck_yeu_cau_muc_ho_tro',
    'CHECK (
        muc_ho_tro_de_xuat >= 0 AND
        (muc_ho_tro_da_thoa_thuan IS NULL OR muc_ho_tro_da_thoa_thuan >= 0)
    )'
);

SELECT route_sharing_add_constraint(
    'chuyen_di',
    'ck_chuyen_di_so_khach',
    'CHECK (
        so_khach_ke_hoach >= 0 AND
        so_khach_thuc_te >= 0 AND
        so_khach_thuc_te <= so_khach_ke_hoach
    )'
);

SELECT route_sharing_add_constraint(
    'diem_dung_hanh_trinh',
    'ck_diem_dung_thu_tu',
    'CHECK (thu_tu >= 0)'
);
SELECT route_sharing_add_constraint(
    'diem_dung_hanh_trinh',
    'ck_diem_dung_ban_kinh',
    'CHECK (ban_kinh_xac_dinh_da_den_met > 0)'
);
SELECT route_sharing_add_constraint(
    'diem_dung_hanh_trinh',
    'ck_diem_dung_booking',
    'CHECK (
        (loai_diem_dung IN (''DRIVER_START'', ''DRIVER_END'') AND yeu_cau_di_chung_id IS NULL)
        OR
        (loai_diem_dung IN (''PICKUP'', ''DROPOFF'') AND yeu_cau_di_chung_id IS NOT NULL)
    )'
);

SELECT route_sharing_add_constraint(
    'ban_ghi_dinh_vi',
    'ck_ban_ghi_thu_tu',
    'CHECK (thu_tu_ban_ghi >= 0)'
);
SELECT route_sharing_add_constraint(
    'ban_ghi_dinh_vi',
    'ck_ban_ghi_do_chinh_xac',
    'CHECK (do_chinh_xac_met IS NULL OR do_chinh_xac_met >= 0)'
);
SELECT route_sharing_add_constraint(
    'ban_ghi_dinh_vi',
    'ck_ban_ghi_toc_do',
    'CHECK (toc_do_met_moi_giay IS NULL OR toc_do_met_moi_giay >= 0)'
);
SELECT route_sharing_add_constraint(
    'ban_ghi_dinh_vi',
    'ck_ban_ghi_huong',
    'CHECK (huong_di_chuyen IS NULL OR huong_di_chuyen BETWEEN 0 AND 360)'
);

SELECT route_sharing_add_constraint(
    'tep_minh_chung',
    'ck_tep_minh_chung_xor',
    'CHECK (
        (khieu_nai_id IS NOT NULL AND su_co_chuyen_di_id IS NULL)
        OR
        (khieu_nai_id IS NULL AND su_co_chuyen_di_id IS NOT NULL)
    )'
);

SELECT route_sharing_add_constraint(
    'quyet_dinh_ky_luat',
    'ck_quyet_dinh_can_cu',
    'CHECK (khieu_nai_id IS NOT NULL OR su_co_chuyen_di_id IS NOT NULL)'
);
SELECT route_sharing_add_constraint(
    'quyet_dinh_ky_luat',
    'ck_quyet_dinh_hieu_luc',
    'CHECK (hieu_luc_den IS NULL OR hieu_luc_den > hieu_luc_tu)'
);

SELECT route_sharing_add_constraint(
    'danh_gia',
    'ck_danh_gia_so_sao',
    'CHECK (so_sao BETWEEN 1 AND 5)'
);
SELECT route_sharing_add_constraint(
    'danh_gia',
    'ck_danh_gia_hai_nguoi',
    'CHECK (nguoi_danh_gia_id <> nguoi_duoc_danh_gia_id)'
);
SELECT route_sharing_add_constraint(
    'bien_lai',
    'ck_bien_lai_gia_tri',
    'CHECK (so_tien_thoa_thuan >= 0 AND khoang_cach_duoc_phuc_vu_met >= 0)'
);

SELECT route_sharing_add_constraint(
    'phien_xac_thuc_tai_khoan',
    'ck_phien_xac_thuc_lan_thu',
    'CHECK (so_lan_thu >= 0 AND so_lan_thu_toi_da > 0 AND so_lan_thu <= so_lan_thu_toi_da)'
);
SELECT route_sharing_add_constraint(
    'phien_xac_thuc_tai_khoan',
    'ck_phien_xac_thuc_han',
    'CHECK (het_han_luc > created_at)'
);

SELECT route_sharing_add_constraint(
    'lan_tham_dinh',
    'ck_lan_tham_dinh_lan_nop',
    'CHECK (lan_nop > 0)'
);

SELECT route_sharing_add_constraint(
    'nhat_ky_phan_quyen',
    'ck_nhat_ky_phan_quyen_du_lieu',
    'CHECK (
        (loai_thao_tac IN (''GAN_NHOM'', ''GO_NHOM'')
            AND nguoi_bi_tac_dong_id IS NOT NULL AND nhom_quyen_id IS NOT NULL)
        OR
        (loai_thao_tac IN (''GAN_QUYEN_TRUC_TIEP'', ''GO_QUYEN_TRUC_TIEP'')
            AND nguoi_bi_tac_dong_id IS NOT NULL AND quyen_han_id IS NOT NULL)
        OR
        (loai_thao_tac IN (''THEM_QUYEN_VAO_NHOM'', ''GO_QUYEN_KHOI_NHOM'')
            AND nhom_quyen_id IS NOT NULL AND quyen_han_id IS NOT NULL)
    )'
);

DROP FUNCTION route_sharing_add_constraint(regclass, text, text);

CREATE UNIQUE INDEX IF NOT EXISTS uk_yeu_cau_hanh_khach_lo_trinh_active
ON yeu_cau_di_chung (hanh_khach_id, lo_trinh_chia_se_id)
WHERE trang_thai_yeu_cau IN ('PENDING', 'ACCEPTED', 'ON_BOARD', 'DISPUTED');
CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_start_moi_chuyen
ON diem_dung_hanh_trinh (chuyen_di_id)
WHERE loai_diem_dung = 'DRIVER_START';

CREATE UNIQUE INDEX IF NOT EXISTS uk_diem_dung_driver_end_moi_chuyen
ON diem_dung_hanh_trinh (chuyen_di_id)
WHERE loai_diem_dung = 'DRIVER_END';

CREATE UNIQUE INDEX IF NOT EXISTS uk_ho_so_thanh_vien_hien_hanh
ON ho_so_thanh_vien (nguoi_dung_id, nha_truong_id, loai_ho_so)
WHERE trang_thai_ho_so IN ('SUBMITTED', 'UNDER_REVIEW', 'NEED_SUPPLEMENT', 'APPROVED', 'SUSPENDED');

CREATE INDEX IF NOT EXISTS gist_lo_trinh_diem_xuat_phat
    ON lo_trinh_chia_se USING GIST (diem_xuat_phat);
CREATE INDEX IF NOT EXISTS gist_lo_trinh_diem_dich
    ON lo_trinh_chia_se USING GIST (diem_dich_tai_xe);
CREATE INDEX IF NOT EXISTS gist_lo_trinh_tuyen_goc
    ON lo_trinh_chia_se USING GIST (tuyen_duong_goc);
CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_don
    ON yeu_cau_di_chung USING GIST (diem_don_thuc_te);
CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_dich
    ON yeu_cau_di_chung USING GIST (diem_dich_cuoi_cung_mong_muon);
CREATE INDEX IF NOT EXISTS gist_yeu_cau_diem_tha
    ON yeu_cau_di_chung USING GIST (diem_tha_thoa_thuan);
CREATE INDEX IF NOT EXISTS gist_yeu_cau_tuyen_mong_muon
    ON yeu_cau_di_chung USING GIST (tuyen_duong_mong_muon_hanh_khach);
CREATE INDEX IF NOT EXISTS gist_yeu_cau_doan_phuc_vu
    ON yeu_cau_di_chung USING GIST (doan_tuyen_duoc_phuc_vu);
CREATE INDEX IF NOT EXISTS gist_chuyen_di_tuyen_van_hanh
    ON chuyen_di USING GIST (tuyen_duong_van_hanh);
CREATE INDEX IF NOT EXISTS gist_chuyen_di_vi_tri_cuoi
    ON chuyen_di USING GIST (vi_tri_cuoi_cung);
CREATE INDEX IF NOT EXISTS gist_diem_dung_ke_hoach
    ON diem_dung_hanh_trinh USING GIST (toa_do_ke_hoach);
CREATE INDEX IF NOT EXISTS gist_ban_ghi_dinh_vi
    ON ban_ghi_dinh_vi USING GIST (toa_do);
CREATE INDEX IF NOT EXISTS gist_su_co_toa_do
    ON su_co_chuyen_di USING GIST (toa_do_xay_ra);
