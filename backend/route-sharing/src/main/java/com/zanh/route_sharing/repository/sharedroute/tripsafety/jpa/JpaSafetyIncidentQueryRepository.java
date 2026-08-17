package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.SafetyIncidentQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentQuerySnapshots;
import com.zanh.route_sharing.security.ClientRequestInfo;
import jakarta.persistence.EntityManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JpaSafetyIncidentQueryRepository implements SafetyIncidentQueryRepository {
        private static final String HANDLE_INCIDENT_PERMISSION = "HANDLE_INCIDENT";
        private final EntityManager entityManager;
        private final SafetyStaffScopeJpaSupport safetyStaffScope;

        public JpaSafetyIncidentQueryRepository(EntityManager entityManager,
                        SafetyStaffScopeJpaSupport safetyStaffScope) {
                this.entityManager = entityManager;
                this.safetyStaffScope = safetyStaffScope;
        }

        @Override
        @Transactional(readOnly = true)
        public SafetyIncidentQuerySnapshots.Queue findQueue(Long actorId, Long schoolId, TrangThaiXuLySuCo status,
                        MucDoSuCo severity, String ownership, int page, int size,
                        LocalDate businessDate) {
                List<Long> actorSchools = safetyStaffScope.findActiveSafetySchoolIds(actorId, businessDate);
                if (schoolId != null) {
                        if (!actorSchools.contains(schoolId))
                                return new SafetyIncidentQuerySnapshots.Queue(List.of(), 0);
                        actorSchools = List.of(schoolId);
                }
                if (actorSchools.isEmpty())
                        return new SafetyIncidentQuerySnapshots.Queue(List.of(), 0);

                StringBuilder where = new StringBuilder(" WHERE ts.school_id IN (:schoolIds) ");
                if (status == null)
                        where.append(" AND i.trang_thai_xu_ly IN ('OPEN','ACKNOWLEDGED','INVESTIGATING') ");
                else
                        where.append(" AND i.trang_thai_xu_ly = :status ");
                if (severity != null)
                        where.append(" AND i.muc_do = :severity ");
                if ("UNASSIGNED".equals(ownership))
                        where.append(" AND i.nguoi_tiep_nhan_id IS NULL ");
                if ("MINE".equals(ownership))
                        where.append(" AND i.nguoi_tiep_nhan_id = :actorId ");

                String base = """
                                FROM su_co_chuyen_di i
                                JOIN (
                                  SELECT DISTINCT b.chuyen_di_id AS trip_id, cfg.nha_truong_id AS school_id
                                  FROM yeu_cau_di_chung b
                                  JOIN cau_hinh_nghiep_vu cfg ON cfg.id = b.cau_hinh_id_luc_gui
                                  WHERE b.chuyen_di_id IS NOT NULL
                                ) ts ON ts.trip_id = i.chuyen_di_id
                                JOIN nha_truong school ON school.id = ts.school_id
                                LEFT JOIN nguoi_dung handler ON handler.id = i.nguoi_tiep_nhan_id
                                """;
                String select = "SELECT i.id, i.chuyen_di_id, school.id, school.ten_truong, i.loai_su_co, i.muc_do, "
                                + "i.trang_thai_xu_ly, i.nguon_phat_hien, i.bao_cao_luc, handler.id, handler.ho_ten, "
                                + "i.tiep_nhan_luc, i.giai_quyet_luc " + base + where
                                + " ORDER BY CASE WHEN i.muc_do='CRITICAL' THEN 0 ELSE 1 END, i.bao_cao_luc ASC, i.id ASC";
                String countSql = "SELECT count(*) " + base + where;

                var q = entityManager.createNativeQuery(select).setParameter("schoolIds", actorSchools)
                                .setFirstResult(page * size).setMaxResults(size);
                var cq = entityManager.createNativeQuery(countSql).setParameter("schoolIds", actorSchools);
                bindQueue(q, actorId, status, severity, ownership);
                bindQueue(cq, actorId, status, severity, ownership);
                List<?> rows = q.getResultList();
                long total = ((Number) cq.getSingleResult()).longValue();
                List<SafetyIncidentQuerySnapshots.QueueItem> items = rows.stream().map(row -> {
                        Object[] r = (Object[]) row;
                        SafetyIncidentQuerySnapshots.Handler handler = r[9] == null ? null
                                        : new SafetyIncidentQuerySnapshots.Handler(num(r[9]), str(r[10]));
                        return new SafetyIncidentQuerySnapshots.QueueItem(
                                        num(r[0]), num(r[1]),
                                        new SafetyIncidentQuerySnapshots.School(num(r[2]), str(r[3])),
                                        str(r[4]), str(r[5]), str(r[6]), str(r[7]), instant(r[8]), handler,
                                        instant(r[11]), instant(r[12]));
                }).toList();
                return new SafetyIncidentQuerySnapshots.Queue(items, total);
        }

        @Override
        @Transactional
        public SafetyIncidentQuerySnapshots.Case findCase(Long actorId, Long incidentId, Instant readAt,
                        LocalDate businessDate, ClientRequestInfo client) {
                SuCoChuyenDi incident = loadProtectedIncident(actorId, incidentId, businessDate, true);
                SafetyIncidentQuerySnapshots.Case result = buildCase(incident, readAt);
                persistReadAudit(actorId, incidentId, "INCIDENT_CASE_VIEW", readAt, client);
                return result;
        }

        @Override
        @Transactional
        public SafetyIncidentQuerySnapshots.Investigation findInvestigationContext(Long actorId, Long incidentId,
                        Instant readAt, LocalDate businessDate,
                        ClientRequestInfo client) {
                SuCoChuyenDi incident = loadProtectedIncident(actorId, incidentId, businessDate, true);
                ChuyenDi trip = incident.getChuyenDi();
                LoTrinhChiaSe route = trip.getLoTrinhChiaSe();
                PhuongTien vehicle = route.getPhuongTien();
                var caseSnapshot = buildCase(incident, readAt);

                List<YeuCauDiChung> bookings = entityManager.createQuery(
                                "select b from YeuCauDiChung b join fetch b.hanhKhach p where b.chuyenDi.id=:tripId order by b.id",
                                YeuCauDiChung.class).setParameter("tripId", trip.getId()).getResultList();
                List<SafetyIncidentQuerySnapshots.Participant> participants = bookings.stream()
                                .map(b -> new SafetyIncidentQuerySnapshots.Participant(b.getHanhKhach().getId(),
                                                b.getHanhKhach().getHoTen(),
                                                "PASSENGER", b.getId(), b.getTrangThaiYeuCau().name(),
                                                b.getChapNhanLuc(), b.getLenXeLuc(),
                                                b.getKhongDenLuc(), b.getXuongXeLuc()))
                                .toList();

                List<DiemDungHanhTrinh> stopEntities = entityManager.createQuery(
                                "select s from DiemDungHanhTrinh s left join fetch s.yeuCauDiChung b where s.chuyenDi.id=:tripId order by s.thuTu, s.id",
                                DiemDungHanhTrinh.class).setParameter("tripId", trip.getId()).getResultList();
                List<SafetyIncidentQuerySnapshots.Stop> stops = stopEntities.stream()
                                .map(s -> new SafetyIncidentQuerySnapshots.Stop(s.getId(), s.getThuTu(),
                                                s.getLoaiDiemDung().name(),
                                                s.getTrangThaiDiemDung().name(),
                                                s.getYeuCauDiChung() == null ? null : s.getYeuCauDiChung().getId(),
                                                position(s.getToaDoKeHoach()), position(s.getToaDoThucTe()),
                                                s.getDenLuc(), s.getBatDauChoLuc(),
                                                s.getHanChoLuc(), s.getHoanThanhLuc()))
                                .toList();

                List<SafetyIncidentQuerySnapshots.TripHistory> tripHistory = entityManager.createQuery(
                                "select h from NhatKyTrangThaiChuyenDi h where h.chuyenDi.id=:tripId order by h.sequence",
                                NhatKyTrangThaiChuyenDi.class)
                                .setParameter("tripId", trip.getId()).getResultList().stream()
                                .map(h -> new SafetyIncidentQuerySnapshots.TripHistory(h.getSequence(),
                                                h.getTrangThaiTruoc().name(),
                                                h.getTrangThaiSau().name(), h.getActor().getId(), h.getOccurredAt(),
                                                h.getReasonCode()))
                                .toList();

                List<SafetyIncidentQuerySnapshots.BookingHistory> bookingHistory = entityManager.createQuery(
                                "select h from NhatKyTrangThaiYeuCau h join fetch h.yeuCauDiChung b where b.chuyenDi.id=:tripId order by b.id,h.sequence",
                                NhatKyTrangThaiYeuCau.class).setParameter("tripId", trip.getId()).getResultList()
                                .stream()
                                .map(h -> new SafetyIncidentQuerySnapshots.BookingHistory(h.getYeuCauDiChung().getId(),
                                                h.getSequence(),
                                                h.getTrangThaiTruoc() == null ? null : h.getTrangThaiTruoc().name(),
                                                h.getTrangThaiSau().name(),
                                                h.getActor().getId(), h.getOccurredAt(), h.getReasonCode()))
                                .toList();

                List<SafetyIncidentQuerySnapshots.MonitoringHistory> monitoring = entityManager.createQuery(
                                "select h from NhatKyGiamSatTinHieu h where h.chuyenDi.id=:tripId order by h.sequence",
                                NhatKyGiamSatTinHieu.class)
                                .setParameter("tripId", trip.getId()).getResultList().stream()
                                .map(h -> new SafetyIncidentQuerySnapshots.MonitoringHistory(h.getSequence(),
                                                h.getTrangThaiTruoc().name(),
                                                h.getTrangThaiSau().name(), h.getTransitionAt(),
                                                h.getSignalReferenceAt(), h.getReasonCode()))
                                .toList();

                long locationCount = entityManager.createQuery(
                                "select count(l) from BanGhiDinhVi l where l.chuyenDi.id=:tripId", Long.class)
                                .setParameter("tripId", trip.getId()).getSingleResult();

                SafetyIncidentQuerySnapshots.Investigation result = new SafetyIncidentQuerySnapshots.Investigation(
                                caseSnapshot.incident(),
                                new SafetyIncidentQuerySnapshots.Trip(trip.getId(), trip.getTrangThaiVanHanh().name(),
                                                trip.getBatDauLuc(), trip.getKetThucLuc()),
                                new SafetyIncidentQuerySnapshots.Route(line(route.getTuyenDuongGoc()),
                                                line(trip.getTuyenDuongVanHanh()),
                                                position(route.getDiemXuatPhat()), position(route.getDiemDichTaiXe())),
                                vehicle(vehicle), participants, stops, tripHistory, bookingHistory, monitoring,
                                caseSnapshot.handling().history(), interventionDetails(incident),
                                new SafetyIncidentQuerySnapshots.LocationEvidenceSummary(locationCount > 0,
                                                locationCount),
                                readAt);
                persistReadAudit(actorId, incidentId, "INCIDENT_INVESTIGATION_CONTEXT_VIEW", readAt, client);
                return result;
        }

        @Override
        @Transactional
        public SafetyIncidentQuerySnapshots.LocationPage findLocationEvidence(Long actorId, Long incidentId,
                        Instant from,
                        Instant to, int page, int size, Instant readAt,
                        LocalDate businessDate, ClientRequestInfo client) {
                SuCoChuyenDi incident = loadProtectedIncident(actorId, incidentId, businessDate, true);
                Long tripId = incident.getChuyenDi().getId();
                String effective = "LEAST(thoi_gian_trinh_duyet, thoi_gian_server_nhan)";
                StringBuilder where = new StringBuilder(" WHERE chuyen_di_id=:tripId ");
                if (from != null)
                        where.append(" AND ").append(effective).append(" >= :from ");
                if (to != null)
                        where.append(" AND ").append(effective).append(" <= :to ");
                String sql = "SELECT thu_tu_ban_ghi, ST_Y(toa_do), ST_X(toa_do), thoi_gian_trinh_duyet, thoi_gian_server_nhan, "
                                + effective + ", do_chinh_xac_met, toc_do_met_moi_giay, huong_di_chuyen, nguon_vi_tri "
                                + "FROM ban_ghi_dinh_vi" + where + " ORDER BY " + effective
                                + " ASC, thu_tu_ban_ghi ASC";
                String countSql = "SELECT count(*) FROM ban_ghi_dinh_vi" + where;
                var q = entityManager.createNativeQuery(sql).setParameter("tripId", tripId).setFirstResult(page * size)
                                .setMaxResults(size);
                var cq = entityManager.createNativeQuery(countSql).setParameter("tripId", tripId);
                if (from != null) {
                        q.setParameter("from", from);
                        cq.setParameter("from", from);
                }
                if (to != null) {
                        q.setParameter("to", to);
                        cq.setParameter("to", to);
                }
                List<SafetyIncidentQuerySnapshots.LocationItem> items = q.getResultList().stream().map(row -> {
                        Object[] r = (Object[]) row;
                        return new SafetyIncidentQuerySnapshots.LocationItem(num(r[0]), decimal(r[1]), decimal(r[2]),
                                        instant(r[3]), instant(r[4]),
                                        instant(r[5]), decimal(r[6]), decimal(r[7]), decimal(r[8]), str(r[9]));
                }).toList();
                long total = ((Number) cq.getSingleResult()).longValue();
                persistReadAudit(actorId, incidentId, "INCIDENT_LOCATION_EVIDENCE_VIEW", readAt, client);
                return new SafetyIncidentQuerySnapshots.LocationPage(items, total);
        }

        @Override
        @Transactional(readOnly = true)
        public SafetyIncidentQuerySnapshots.Eligible findEligibleHandlers(Long actorId, Long incidentId, int page,
                        int size,
                        LocalDate businessDate) {
                SuCoChuyenDi incident = loadScopedIncident(actorId, incidentId, businessDate);
                Long schoolId = safetyStaffScope.resolveTripSchoolId(incident.getChuyenDi().getId());
                List<Long> ids = safetyStaffScope.findEligibleUserIds(schoolId, businessDate,
                                HANDLE_INCIDENT_PERMISSION);
                long total = ids.size();
                int from = Math.min(page * size, ids.size());
                int to = Math.min(from + size, ids.size());
                List<Long> pageIds = ids.subList(from, to);
                if (pageIds.isEmpty())
                        return new SafetyIncidentQuerySnapshots.Eligible(List.of(), total);
                Map<Long, NguoiDung> users = entityManager
                                .createQuery("select u from NguoiDung u where u.id in :ids", NguoiDung.class)
                                .setParameter("ids", pageIds).getResultList().stream()
                                .collect(java.util.stream.Collectors.toMap(NguoiDung::getId, u -> u));
                Long current = incident.getNguoiTiepNhan() == null ? null : incident.getNguoiTiepNhan().getId();
                List<SafetyIncidentQuerySnapshots.EligibleItem> items = pageIds.stream().map(id -> {
                        NguoiDung u = users.get(id);
                        if (u == null)
                                throw SafetyStaffScopeJpaSupport.invariantViolation();
                        return new SafetyIncidentQuerySnapshots.EligibleItem(id, u.getHoTen(),
                                        Objects.equals(id, current));
                }).toList();
                return new SafetyIncidentQuerySnapshots.Eligible(items, total);
        }

        @Override
        @Transactional(readOnly = true)
        public SafetyIncidentQuerySnapshots.ReporterStatus findReporterStatus(Long actorId, Long tripId,
                        Long incidentId) {
                SuCoChuyenDi i = entityManager.createQuery(
                                "select i from SuCoChuyenDi i left join fetch i.nguoiBaoCao r join fetch i.chuyenDi t "
                                                + "where i.id=:incidentId and t.id=:tripId",
                                SuCoChuyenDi.class)
                                .setParameter("incidentId", incidentId).setParameter("tripId", tripId).setMaxResults(1)
                                .getResultList().stream().findFirst()
                                .orElseThrow(SafetyStaffScopeJpaSupport::safetyIncidentNotFound);
                if (i.getNguoiBaoCao() == null || !Objects.equals(i.getNguoiBaoCao().getId(), actorId))
                        throw SafetyStaffScopeJpaSupport.safetyIncidentNotFound();
                SafetyIncidentQuerySnapshots.ReporterIntervention intervention = reporterIntervention(i, actorId);
                return new SafetyIncidentQuerySnapshots.ReporterStatus(i.getId(), tripId, i.getLoaiSuCo().name(),
                                i.getMucDo().name(),
                                i.getBaoCaoLuc(), i.getTrangThaiXuLy().name(), i.getTiepNhanLuc(), i.getGiaiQuyetLuc(),
                                i.getKetLuan(), intervention);
        }

        private SuCoChuyenDi loadProtectedIncident(Long actorId, Long incidentId, LocalDate businessDate,
                        boolean handlerOnly) {
                SuCoChuyenDi incident = loadScopedIncident(actorId, incidentId, businessDate);
                if (handlerOnly && (incident.getNguoiTiepNhan() == null
                                || !Objects.equals(incident.getNguoiTiepNhan().getId(), actorId))) {
                        throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_NOT_ASSIGNED_TO_ACTOR",
                                        "Actor không phải người đang phụ trách incident.");
                }
                return incident;
        }

        private SuCoChuyenDi loadScopedIncident(Long actorId, Long incidentId, LocalDate businessDate) {
                SuCoChuyenDi incident = entityManager.createQuery(
                                "select i from SuCoChuyenDi i join fetch i.chuyenDi t join fetch t.loTrinhChiaSe r "
                                                + "join fetch r.taiXe d join fetch r.phuongTien v join fetch v.dongXe m join fetch m.hangXe b "
                                                + "left join fetch i.nguoiBaoCao reporter left join fetch i.nguoiBiBaoCao target "
                                                + "left join fetch i.nguoiTiepNhan handler where i.id=:id",
                                SuCoChuyenDi.class)
                                .setParameter("id", incidentId).setMaxResults(1).getResultList().stream().findFirst()
                                .orElseThrow(SafetyStaffScopeJpaSupport::safetyIncidentNotFound);
                Long schoolId = safetyStaffScope.resolveTripSchoolId(incident.getChuyenDi().getId());
                safetyStaffScope.requireActiveSafetyStaffScope(actorId, schoolId, businessDate);
                return incident;
        }

        private SafetyIncidentQuerySnapshots.Case buildCase(SuCoChuyenDi i, Instant readAt) {
                ChuyenDi trip = i.getChuyenDi();
                LoTrinhChiaSe route = trip.getLoTrinhChiaSe();
                List<SafetyIncidentQuerySnapshots.History> history = handlingHistory(i.getId());
                String reporterRole = i.getNguonPhatHien().name();
                SafetyIncidentQuerySnapshots.Person reporter = person(i.getNguoiBaoCao(), reporterRole);
                String targetRole = i.getNguoiBiBaoCao() == null ? null
                                : (Objects.equals(i.getNguoiBiBaoCao().getId(), route.getTaiXe().getId()) ? "DRIVER"
                                                : "PASSENGER");
                SafetyIncidentQuerySnapshots.Incident incident = new SafetyIncidentQuerySnapshots.Incident(i.getId(),
                                trip.getId(), i.getLoaiSuCo().name(),
                                i.getMucDo().name(), i.getTrangThaiXuLy().name(), i.getNguonPhatHien().name(),
                                i.getBaoCaoLuc(), i.getNoiDung(),
                                reporter, person(i.getNguoiBiBaoCao(), targetRole),
                                new SafetyIncidentQuerySnapshots.OperationalSnapshot(
                                                position(i.getToaDoThamChieuVanHanh()), i.getViTriThamChieuQuanSatLuc(),
                                                i.getViTriThamChieuNhanLuc(),
                                                i.getTrangThaiGiamSatLucBaoCao() == null ? null
                                                                : i.getTrangThaiGiamSatLucBaoCao().name(),
                                                i.getTinHieuThamChieuLuc()));
                SafetyIncidentQuerySnapshots.TripContext tripContext = new SafetyIncidentQuerySnapshots.TripContext(
                                trip.getId(),
                                trip.getTrangThaiVanHanh().name(), trip.getBatDauLuc(), trip.getKetThucLuc(),
                                person(route.getTaiXe(), "DRIVER"), vehicle(route.getPhuongTien()));
                SafetyIncidentQuerySnapshots.Handling handling = new SafetyIncidentQuerySnapshots.Handling(
                                person(i.getNguoiTiepNhan(), "SAFETY"),
                                i.getTiepNhanLuc(), i.getGiaiQuyetLuc(), i.getKetLuan(), history);
                return new SafetyIncidentQuerySnapshots.Case(incident, tripContext, handling, compactInterventions(i),
                                readAt);
        }

        private List<CanThiepAnToanChuyenDi> interventionEntities(Long incidentId) {
                return entityManager.createQuery(
                                "select c from CanThiepAnToanChuyenDi c left join fetch c.yeuCauMucTieu b "
                                                + "left join fetch b.hanhKhach p join fetch c.nguoiKhoiTao a left join fetch c.nguoiKetThuc f "
                                                + "where c.suCoChuyenDi.id=:id order by c.thuTuCanThiep",
                                CanThiepAnToanChuyenDi.class)
                                .setParameter("id", incidentId).getResultList();
        }

        private List<SafetyIncidentQuerySnapshots.CompactIntervention> compactInterventions(SuCoChuyenDi incident) {
                LoTrinhChiaSe route = incident.getChuyenDi().getLoTrinhChiaSe();
                return interventionEntities(incident.getId()).stream().map(c -> {
                        SafetyIncidentQuerySnapshots.Person target = c.getYeuCauMucTieu() == null ? null
                                        : new SafetyIncidentQuerySnapshots.Person(
                                                        c.getYeuCauMucTieu().getHanhKhach().getId(),
                                                        c.getYeuCauMucTieu().getHanhKhach().getHoTen(), "PASSENGER");
                        return new SafetyIncidentQuerySnapshots.CompactIntervention(c.getId(),
                                        c.getLoaiCanThiep().name(),
                                        c.getTrangThaiCanThiep().name(), target, c.getKhoiTaoLuc(), c.getKetThucLuc());
                }).toList();
        }

        private List<SafetyIncidentQuerySnapshots.Intervention> interventionDetails(SuCoChuyenDi incident) {
                ChuyenDi trip = incident.getChuyenDi();
                LoTrinhChiaSe route = trip.getLoTrinhChiaSe();
                return interventionEntities(incident.getId()).stream().map(c -> {
                        SafetyIncidentQuerySnapshots.InterventionTarget target = c.getYeuCauMucTieu() == null ? null
                                        : new SafetyIncidentQuerySnapshots.InterventionTarget(
                                                        c.getYeuCauMucTieu().getId(),
                                                        c.getYeuCauMucTieu().getHanhKhach().getId(),
                                                        c.getYeuCauMucTieu().getHanhKhach().getHoTen(), "PASSENGER");
                        SafetyIncidentQuerySnapshots.InterventionActor initiator = interventionActor(
                                        c.getNguoiKhoiTao(), route);
                        SafetyIncidentQuerySnapshots.InterventionActor finisher = c.getNguoiKetThuc() == null ? null
                                        : interventionActor(c.getNguoiKetThuc(), route);
                        List<SafetyIncidentQuerySnapshots.StopImpact> impacts = entityManager.createQuery(
                                        "select d from ChiTietCanThiepDiemDung d join fetch d.diemDungHanhTrinh s "
                                                        + "left join fetch s.yeuCauDiChung b where d.canThiepAnToanChuyenDi.id=:id order by s.thuTu,s.id",
                                        ChiTietCanThiepDiemDung.class).setParameter("id", c.getId()).getResultList()
                                        .stream().map(d -> {
                                                DiemDungHanhTrinh stop = d.getDiemDungHanhTrinh();
                                                return new SafetyIncidentQuerySnapshots.StopImpact(stop.getId(),
                                                                stop.getYeuCauDiChung() == null ? null
                                                                                : stop.getYeuCauDiChung().getId(),
                                                                stop.getLoaiDiemDung().name(),
                                                                d.getTrangThaiTruoc().name(),
                                                                d.getTrangThaiSau().name(), d.getHanChoLucTruoc(),
                                                                d.getHanChoLucSau(), d.getXayRaLuc());
                                        }).toList();
                        return new SafetyIncidentQuerySnapshots.Intervention(c.getId(), incident.getId(), trip.getId(),
                                        c.getLoaiCanThiep().name(), c.getTrangThaiCanThiep().name(), target, initiator,
                                        c.getKhoiTaoLuc(), finisher, c.getKetThucLuc(),
                                        c.getLyDoAnToan(), c.getXuongXeKhanCapLuc(),
                                        position(c.getToaDoXuongXeKhanCap()),
                                        c.getSoKhachThucTeLucBatDau(), c.getSoKhachThucTeLucKetThuc(),
                                        c.getSoXacThucLenXeVoHieuHoa(), impacts);
                }).toList();
        }

        private SafetyIncidentQuerySnapshots.InterventionActor interventionActor(NguoiDung actor, LoTrinhChiaSe route) {
                String role = Objects.equals(actor.getId(), route.getTaiXe().getId()) ? "DRIVER"
                                : isTripPassenger(route.getId(), actor.getId()) ? "PASSENGER" : "SAFETY";
                return new SafetyIncidentQuerySnapshots.InterventionActor(actor.getId(), actor.getHoTen(), role);
        }

        private boolean isTripPassenger(Long routeId, Long userId) {
                Long count = entityManager.createQuery(
                                "select count(b) from YeuCauDiChung b where b.loTrinhChiaSe.id=:routeId and b.hanhKhach.id=:userId",
                                Long.class)
                                .setParameter("routeId", routeId).setParameter("userId", userId).getSingleResult();
                return count != null && count > 0;
        }

        private SafetyIncidentQuerySnapshots.ReporterIntervention reporterIntervention(SuCoChuyenDi incident,
                        Long reporterId) {
                List<CanThiepAnToanChuyenDi> interventions = interventionEntities(incident.getId());
                if (interventions.isEmpty())
                        return null;
                CanThiepAnToanChuyenDi c = interventions.get(interventions.size() - 1);
                String ownBookingStatus = null;
                List<YeuCauDiChung> own = entityManager.createQuery(
                                "select b from YeuCauDiChung b where b.chuyenDi.id=:tripId and b.hanhKhach.id=:userId",
                                YeuCauDiChung.class)
                                .setParameter("tripId", incident.getChuyenDi().getId())
                                .setParameter("userId", reporterId).getResultList();
                if (own.size() > 1)
                        throw SafetyStaffScopeJpaSupport.invariantViolation();
                if (!own.isEmpty() && (c.getYeuCauMucTieu() == null
                                || Objects.equals(c.getYeuCauMucTieu().getId(), own.get(0).getId()))) {
                        ownBookingStatus = own.get(0).getTrangThaiYeuCau().name();
                }
                Instant changedAt = c.getKetThucLuc() == null ? c.getKhoiTaoLuc() : c.getKetThucLuc();
                return new SafetyIncidentQuerySnapshots.ReporterIntervention(c.getId(), c.getLoaiCanThiep().name(),
                                c.getTrangThaiCanThiep().name(), c.getChuyenDi().getTrangThaiVanHanh().name(),
                                ownBookingStatus, changedAt);
        }

        private List<SafetyIncidentQuerySnapshots.History> handlingHistory(Long incidentId) {
                return entityManager.createQuery(
                                "select h from NhatKyXuLySuCo h left join fetch h.nguoiPhuTrachTruoc p left join fetch h.nguoiPhuTrachSau n "
                                                + "join fetch h.actor a where h.suCoChuyenDi.id=:id order by h.sequence",
                                NhatKyXuLySuCo.class)
                                .setParameter("id", incidentId).getResultList().stream()
                                .map(h -> new SafetyIncidentQuerySnapshots.History(
                                                h.getSequence(), h.getThaoTac().name(), h.getTrangThaiTruoc().name(),
                                                h.getTrangThaiSau().name(),
                                                person(h.getNguoiPhuTrachTruoc(), "SAFETY"),
                                                person(h.getNguoiPhuTrachSau(), "SAFETY"),
                                                person(h.getActor(), "SAFETY"),
                                                h.getOccurredAt(), h.getReason(), h.getSafeConclusionSnapshot()))
                                .toList();
        }

        private void persistReadAudit(Long actorId, Long incidentId, String purpose, Instant readAt,
                        ClientRequestInfo client) {
                NguoiDung actor = entityManager.find(NguoiDung.class, actorId);
                if (actor == null)
                        throw SafetyStaffScopeJpaSupport.safetyIncidentNotFound();
                String ip = client == null || client.ipAddress() == null || client.ipAddress().isBlank() ? "UNKNOWN"
                                : client.ipAddress();
                String ua = client == null || client.userAgent() == null || client.userAgent().isBlank() ? "UNKNOWN"
                                : client.userAgent();
                entityManager.persist(
                                NhatKyTruyCapDuLieuNhayCam.builder().loaiTaiNguyen(LoaiTaiNguyenNhayCam.SU_CO_CHUYEN_DI)
                                                .taiNguyenId(incidentId).hanhDong(HanhDongTruyCap.VIEW).mucDich(purpose)
                                                .truyCapLuc(readAt)
                                                .diaChiIp(ip).thongTinTrinhDuyet(ua).nguoiTruyCap(actor).build());
                entityManager.flush();
        }

        private static void bindQueue(jakarta.persistence.Query q, Long actorId, TrangThaiXuLySuCo status,
                        MucDoSuCo severity, String ownership) {
                if (status != null)
                        q.setParameter("status", status.name());
                if (severity != null)
                        q.setParameter("severity", severity.name());
                if ("MINE".equals(ownership))
                        q.setParameter("actorId", actorId);
        }

        private static SafetyIncidentQuerySnapshots.Person person(NguoiDung u, String role) {
                return u == null ? null : new SafetyIncidentQuerySnapshots.Person(u.getId(), u.getHoTen(), role);
        }

        private static SafetyIncidentQuerySnapshots.Vehicle vehicle(PhuongTien v) {
                String display = v.getDongXe().getHangXe().getTenHang() + " " + v.getDongXe().getTenDongXe();
                return new SafetyIncidentQuerySnapshots.Vehicle(v.getId(), v.getBienSoXe(), display.trim());
        }

        private static SafetyIncidentQuerySnapshots.Position position(Point p) {
                return p == null ? null
                                : new SafetyIncidentQuerySnapshots.Position(BigDecimal.valueOf(p.getY()),
                                                BigDecimal.valueOf(p.getX()));
        }

        private static List<SafetyIncidentQuerySnapshots.Position> line(LineString l) {
                if (l == null)
                        return List.of();
                List<SafetyIncidentQuerySnapshots.Position> out = new ArrayList<>();
                for (Coordinate c : l.getCoordinates())
                        out.add(new SafetyIncidentQuerySnapshots.Position(BigDecimal.valueOf(c.y),
                                        BigDecimal.valueOf(c.x)));
                return List.copyOf(out);
        }

        private static Long num(Object v) {
                return v == null ? null : ((Number) v).longValue();
        }

        private static String str(Object v) {
                return v == null ? null : v.toString();
        }

        private static BigDecimal decimal(Object v) {
                if (v == null)
                        return null;
                return v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
        }

        private static Instant instant(Object v) {
                if (v == null)
                        return null;
                if (v instanceof Instant i)
                        return i;
                if (v instanceof Timestamp t)
                        return t.toInstant();
                if (v instanceof java.time.OffsetDateTime o)
                        return o.toInstant();
                return Instant.parse(v.toString());
        }
}
