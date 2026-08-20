package com.zanh.route_sharing.storage.evidence;

import java.io.IOException;
import java.io.InputStream;

public interface EvidenceBinaryStorage {
    StagedBinary stage(InputStream inputStream) throws IOException;

    PromotionResult promote(StagedBinary staged, String storageKey) throws IOException;

    VerifiedBinary verify(String storageKey, long expectedSize, String expectedSha256) throws IOException;

    void cleanupStage(StagedBinary staged);

    void deleteFinal(String storageKey);
}
