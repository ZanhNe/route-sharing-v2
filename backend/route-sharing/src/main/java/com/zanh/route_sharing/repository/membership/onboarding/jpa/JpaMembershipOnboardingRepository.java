package com.zanh.route_sharing.repository.membership.onboarding.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.membership.onboarding.*;
import com.zanh.route_sharing.repository.membership.onboarding.model.*;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class JpaMembershipOnboardingRepository implements MembershipOnboardingContextRepository,
        MembershipProfileRepository, MembershipSubmissionRepository, MembershipEvidenceRepository,
        MembershipAcademicContextRepository {

    private static final Set<TrangThaiHoSoThanhVien> CURRENT_STATES = EnumSet.of(
            TrangThaiHoSoThanhVien.DRAFT,
            TrangThaiHoSoThanhVien.SUBMITTED,
            TrangThaiHoSoThanhVien.UNDER_REVIEW,
            TrangThaiHoSoThanhVien.NEED_SUPPLEMENT,
            TrangThaiHoSoThanhVien.APPROVED,
            TrangThaiHoSoThanhVien.SUSPENDED);

    private final EntityManager entityManager;

    public JpaMembershipOnboardingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipOnboardingContext requireReadContext(Long actorId) {
        NguoiDung account = entityManager.find(NguoiDung.class, actorId);
        if (account == null) throw predecessorInvariant();
        return new MembershipOnboardingContext(account, requireAuthoritativeSchool(actorId));
    }

    @Override
    public MembershipOnboardingContext lockContext(Long actorId) {
        try {
            NguoiDung account = entityManager.find(NguoiDung.class, actorId, LockModeType.PESSIMISTIC_WRITE);
            if (account == null) throw predecessorInvariant();
            return new MembershipOnboardingContext(account, requireAuthoritativeSchool(actorId));
        } catch (PessimisticLockException | LockTimeoutException ex) {
            throw concurrent();
        }
    }

    @Override
    public NhaTruong lockSchool(Long schoolId) {
        try {
            NhaTruong school = entityManager.find(NhaTruong.class, schoolId, LockModeType.PESSIMISTIC_WRITE);
            if (school == null) throw predecessorInvariant();
            return school;
        } catch (PessimisticLockException | LockTimeoutException ex) {
            throw concurrent();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HoSoThanhVien> findCurrent(Long actorId, Long schoolId) {
        return currentRows(actorId, schoolId, null).stream().findFirst();
    }

    @Override
    public Optional<HoSoThanhVien> lockCurrent(Long actorId, Long schoolId) {
        try {
            // Lock only the owning parent table row first. Applying PESSIMISTIC_WRITE to a
            // polymorphic JOINED-inheritance query can make PostgreSQL try FOR UPDATE across
            // nullable subtype joins. The parent row is the aggregate ownership/uniqueness row.
            @SuppressWarnings("unchecked")
            List<Number> ids = entityManager.createNativeQuery(
                            "select id from ho_so_thanh_vien "
                                    + "where nguoi_dung_id=:actor and nha_truong_id=:school "
                                    + "and trang_thai_ho_so in ('DRAFT','SUBMITTED','UNDER_REVIEW','NEED_SUPPLEMENT','APPROVED','SUSPENDED') "
                                    + "order by id asc limit 2 for update")
                    .setParameter("actor", actorId)
                    .setParameter("school", schoolId)
                    .getResultList();
            if (ids.size() > 1) throw lifecycleConflict();
            if (ids.isEmpty()) return Optional.empty();
            Long profileId = ids.get(0).longValue();
            // The parent row is already locked above. Load the polymorphic aggregate without
            // another lock so Hibernate may resolve the JOINED subtype without emitting a
            // PostgreSQL FOR UPDATE over nullable subtype joins.
            List<HoSoThanhVien> profiles = entityManager.createQuery(
                            "select h from HoSoThanhVien h where h.id=:profileId", HoSoThanhVien.class)
                    .setParameter("profileId", profileId)
                    .setMaxResults(2)
                    .getResultList();
            if (profiles.size() != 1) throw lifecycleConflict();
            return Optional.of(profiles.get(0));
        } catch (PessimisticLockException | LockTimeoutException ex) {
            throw concurrent();
        }
    }

    private List<HoSoThanhVien> currentRows(Long actorId, Long schoolId, LockModeType lockMode) {
        TypedQuery<HoSoThanhVien> query = entityManager.createQuery(
                "select h from HoSoThanhVien h where h.nguoiDung.id=:actor and h.nhaTruong.id=:school "
                        + "and h.trangThaiHoSo in :states order by h.id asc", HoSoThanhVien.class)
                .setParameter("actor", actorId)
                .setParameter("school", schoolId)
                .setParameter("states", CURRENT_STATES)
                .setMaxResults(2);
        if (lockMode != null) query.setLockMode(lockMode);
        List<HoSoThanhVien> rows = query.getResultList();
        if (rows.size() > 1) throw lifecycleConflict();
        return rows;
    }

    @Override
    public HoSoSinhVien persistNew(HoSoSinhVien profile) {
        try {
            entityManager.persist(profile);
            entityManager.flush();
            return profile;
        } catch (PersistenceException ex) {
            throw lifecycleConflict();
        }
    }

    @Override
    public void forceVersionIncrement(HoSoThanhVien profile) {
        try {
            entityManager.lock(profile, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) {
            throw concurrent();
        }
    }

    @Override
    public void flush() {
        try {
            entityManager.flush();
        } catch (OptimisticLockException ex) {
            throw concurrent();
        } catch (PersistenceException ex) {
            throw dataIntegrity();
        }
    }

    @Override
    public boolean studentCodeReservedByOtherIdentity(Long schoolId, String normalizedStudentCode, Long currentProfileId) {
        // BR-MEM01-08: MEM-01 never reassigns a student code that already belongs to a
        // historical identity. Recovery/reassignment is a separate privileged workflow.
        String jpql = "select count(s) from HoSoSinhVien s where s.nhaTruong.id=:school "
                + "and upper(s.maSoSinhVien)=:code";
        if (currentProfileId != null) jpql += " and s.id<>:profileId";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("school", schoolId)
                .setParameter("code", normalizedStudentCode);
        if (currentProfileId != null) query.setParameter("profileId", currentProfileId);
        return query.getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LanNopHoSoThanhVien> findInitial(Long profileId) {
        List<LanNopHoSoThanhVien> rows = entityManager.createQuery(
                "select distinct s from LanNopHoSoThanhVien s left join fetch s.bangChungDaNop "
                        + "where s.hoSoThanhVien.id=:profileId and s.lanNop=1", LanNopHoSoThanhVien.class)
                .setParameter("profileId", profileId)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() > 1) throw invariant();
        return rows.stream().findFirst();
    }

    @Override
    public LanNopHoSoThanhVien persist(LanNopHoSoThanhVien submission) {
        try {
            entityManager.persist(submission);
            entityManager.flush();
            return submission;
        } catch (PersistenceException ex) {
            throw dataIntegrity();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BangChungThanhVien> findCurrent(Long profileId) {
        return entityManager.createQuery(
                "select e from BangChungThanhVien e where e.hoSoThanhVien.id=:profileId and e.current=true "
                        + "order by e.slot asc, e.id asc", BangChungThanhVien.class)
                .setParameter("profileId", profileId)
                .getResultList();
    }

    @Override
    public BangChungThanhVien persist(BangChungThanhVien evidence) {
        try {
            entityManager.persist(evidence);
            entityManager.flush();
            return evidence;
        } catch (PersistenceException ex) {
            throw dataIntegrity();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MembershipEvidenceBinaryMetadata> findOwnedBinary(Long actorId, Long evidenceId) {
        List<BangChungThanhVien> rows = entityManager.createQuery(
                "select e from BangChungThanhVien e join fetch e.hoSoThanhVien h "
                        + "where e.id=:evidenceId and h.nguoiDung.id=:actorId", BangChungThanhVien.class)
                .setParameter("evidenceId", evidenceId)
                .setParameter("actorId", actorId)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() > 1) throw invariant();
        return rows.stream().findFirst().map(e -> new MembershipEvidenceBinaryMetadata(
                e.getId(), e.getOriginalFilename(), e.getVerifiedMediaType(), e.getSizeBytes(), e.getSha256(), e.getStorageKey()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lop> findClassInSchool(Long classId, Long schoolId) {
        List<Lop> rows = entityManager.createQuery(
                "select l from Lop l join fetch l.nganh n join fetch n.donViTruong d join fetch d.nhaTruong s "
                        + "where l.id=:classId and s.id=:schoolId", Lop.class)
                .setParameter("classId", classId)
                .setParameter("schoolId", schoolId)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() > 1) throw invariant();
        return rows.stream().findFirst();
    }

    private NhaTruong requireAuthoritativeSchool(Long actorId) {
        List<Long> schoolIds = entityManager.createQuery(
                "select distinct v.nhaTruong.id from DongYPhapLy d join d.vanBanPhapLy v "
                        + "where d.nguoiDung.id=:actorId order by v.nhaTruong.id", Long.class)
                .setParameter("actorId", actorId)
                .setMaxResults(2)
                .getResultList();
        if (schoolIds.size() != 1) throw predecessorInvariant();
        NhaTruong school = entityManager.find(NhaTruong.class, schoolIds.get(0));
        if (school == null) throw predecessorInvariant();
        return school;
    }

    private static BusinessException predecessorInvariant() {
        return new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_PREDECESSOR_INVARIANT_VIOLATION",
                "Không thể xác định duy nhất ngữ cảnh trường từ hành trình đăng ký đã xác nhận.");
    }

    private static BusinessException lifecycleConflict() {
        return new BusinessException(HttpStatus.CONFLICT, "MEMBERSHIP_CURRENT_LIFECYCLE_CONFLICT",
                "Hồ sơ thành viên hiện tại xung đột với trạng thái onboarding.");
    }

    private static BusinessException concurrent() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Hồ sơ vừa được thay đổi đồng thời. Vui lòng tải lại và thử lại.");
    }

    private static BusinessException dataIntegrity() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Hồ sơ xung đột với ràng buộc dữ liệu.");
    }

    private static BusinessException invariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBERSHIP_CONTEXT_INVARIANT_VIOLATION",
                "Dữ liệu hồ sơ thành viên không nhất quán.");
    }
}
