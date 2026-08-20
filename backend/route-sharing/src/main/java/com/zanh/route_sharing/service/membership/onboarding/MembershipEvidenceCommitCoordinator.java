package com.zanh.route_sharing.service.membership.onboarding;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.membership.onboarding.MembershipEvidenceRepository;
import com.zanh.route_sharing.service.membership.onboarding.model.PreparedMembershipEvidence;
import com.zanh.route_sharing.storage.evidence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.HexFormat;

@Component
public class MembershipEvidenceCommitCoordinator {
    private final MembershipEvidenceRepository repository;
    private final EvidenceBinaryStorage storage;
    private final MembershipPolicy policy;

    public MembershipEvidenceCommitCoordinator(MembershipEvidenceRepository repository,
            EvidenceBinaryStorage storage, MembershipPolicy policy) {
        this.repository = repository; this.storage = storage; this.policy = policy;
    }

    public Set<ViTriBangChungThanhVien> effectiveSlots(List<BangChungThanhVien> current,
            List<PreparedMembershipEvidence> prepared, Set<ViTriBangChungThanhVien> removals) {
        EnumSet<ViTriBangChungThanhVien> slots = EnumSet.noneOf(ViTriBangChungThanhVien.class);
        current.stream().filter(BangChungThanhVien::isCurrent).forEach(e -> slots.add(e.getSlot()));
        if (removals != null) removals.forEach(slots::remove);
        prepared.forEach(item -> slots.add(item.slot()));
        return Collections.unmodifiableSet(slots);
    }

    public List<BangChungThanhVien> apply(HoSoSinhVien profile, List<BangChungThanhVien> current,
            List<PreparedMembershipEvidence> prepared, Set<ViTriBangChungThanhVien> removals) {
        Set<ViTriBangChungThanhVien> supplied = EnumSet.noneOf(ViTriBangChungThanhVien.class);
        prepared.forEach(p -> supplied.add(p.slot()));
        policy.validateSlotMutation(removals, supplied);

        EnumMap<ViTriBangChungThanhVien, BangChungThanhVien> effective = new EnumMap<>(ViTriBangChungThanhVien.class);
        current.stream().filter(BangChungThanhVien::isCurrent).forEach(e -> effective.put(e.getSlot(), e));

        boolean deactivatedExistingRow = false;
        for (ViTriBangChungThanhVien slot : removals) {
            BangChungThanhVien old = effective.remove(slot);
            if (old != null) {
                old.setCurrent(false);
                deactivatedExistingRow = true;
            }
        }

        List<PreparedMembershipEvidence> replacements = new ArrayList<>();
        for (PreparedMembershipEvidence item : prepared) {
            BangChungThanhVien old = effective.get(item.slot());
            if (old != null && old.getSha256().equals(item.staged().sha256Hex())
                    && old.getVerifiedMediaType().equals(item.verifiedMediaType())) {
                continue;
            }
            if (old != null) {
                old.setCurrent(false);
                effective.remove(item.slot());
                deactivatedExistingRow = true;
            }
            replacements.add(item);
        }

        // Hibernate executes INSERT actions before UPDATE actions during a normal flush. The
        // partial unique index permits only one current row per (profile, slot), therefore a
        // replacement must flush the old row's is_current=false transition before inserting
        // the new current row. This is still inside the same short transaction and rolls back
        // atomically if the later binary promotion/metadata insert fails.
        if (deactivatedExistingRow) repository.flush();

        for (PreparedMembershipEvidence item : replacements) {
            String key = storageKey(profile.getId(), item.slot(), item.staged().sha256Hex());
            PromotionResult promotion;
            try {
                promotion = storage.promote(item.staged(), key);
                if (!promotion.createdNew()) storage.verify(key, item.staged().sizeBytes(), item.staged().sha256Hex());
            } catch (IOException ex) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EVIDENCE_STORAGE_UNAVAILABLE",
                        "Kho bằng chứng hiện không khả dụng.");
            }
            if (promotion.createdNew()) registerRollbackCleanup(key);
            BangChungThanhVien evidence = BangChungThanhVien.builder()
                    .hoSoThanhVien(profile)
                    .slot(item.slot())
                    .originalFilename(item.originalFilename())
                    .verifiedMediaType(item.verifiedMediaType())
                    .sizeBytes(item.staged().sizeBytes())
                    .sha256(item.staged().sha256Hex())
                    .storageKey(key)
                    .reviewState(TrangThaiBangChungThanhVien.PENDING)
                    .current(true)
                    .build();
            repository.persist(evidence);
            effective.put(item.slot(), evidence);
        }
        return effective.values().stream().sorted(Comparator.comparing(e -> e.getSlot().name())).toList();
    }

    static String storageKey(Long profileId, ViTriBangChungThanhVien slot, String sha256) {
        if (profileId == null || slot == null || sha256 == null) throw new IllegalArgumentException("Evidence key context không hợp lệ.");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = profileId + ":" + slot.name() + ":" + sha256;
            String objectHash = HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)));
            return objectHash.substring(0, 2) + "/" + objectHash;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng.", ex);
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.deleteFinal(storageKey);
            }
        });
    }
}
