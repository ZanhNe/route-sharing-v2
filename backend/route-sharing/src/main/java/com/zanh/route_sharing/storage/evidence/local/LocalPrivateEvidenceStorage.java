package com.zanh.route_sharing.storage.evidence.local;

import com.zanh.route_sharing.config.properties.EvidenceStorageProperties;
import com.zanh.route_sharing.storage.evidence.*;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class LocalPrivateEvidenceStorage implements EvidenceBinaryStorage {
    private static final int BUFFER_SIZE = 64 * 1024;
    private final Path root;
    private final Path stagingRoot;
    private final Path objectRoot;

    public LocalPrivateEvidenceStorage(EvidenceStorageProperties properties) {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        this.stagingRoot = root.resolve(".staging").normalize();
        this.objectRoot = root.resolve("objects").normalize();
        requireUnderRoot(stagingRoot);
        requireUnderRoot(objectRoot);
    }

    @Override
    public StagedBinary stage(InputStream inputStream) throws IOException {
        if (inputStream == null)
            throw new IOException("Binary stream không tồn tại.");
        Files.createDirectories(stagingRoot);
        requireNoSymlinkComponents(stagingRoot);
        Path temp = Files.createTempFile(stagingRoot, "evidence-", ".stage");
        MessageDigest digest = sha256();
        long size = 0;
        try (InputStream in = inputStream; OutputStream out = Files.newOutputStream(temp, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0)
                    continue;
                out.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
            }
        } catch (IOException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }
        if (size <= 0) {
            Files.deleteIfExists(temp);
            throw new IOException("Binary evidence rỗng.");
        }
        return new StagedBinary(temp, size, HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public PromotionResult promote(StagedBinary staged, String storageKey) throws IOException {
        Path source = requireStage(staged);
        Path target = resolveStorageKey(storageKey);
        Files.createDirectories(target.getParent());
        requireNoSymlinkComponents(target.getParent());
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) {
            throw new IOException("Evidence storage target không được là symbolic link.");
        }
        if (Files.exists(target)) {
            verify(target, staged.sizeBytes(), staged.sha256Hex());
            return new PromotionResult(false);
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        } catch (FileAlreadyExistsException ex) {
            verify(target, staged.sizeBytes(), staged.sha256Hex());
            return new PromotionResult(false);
        }
        return new PromotionResult(true);
    }

    @Override
    public VerifiedBinary verify(String storageKey, long expectedSize, String expectedSha256) throws IOException {
        Path path = resolveStorageKey(storageKey);
        requireNoSymlinkComponents(path);
        verify(path, expectedSize, expectedSha256);
        return new VerifiedBinary(new PathResource(path), expectedSize, expectedSha256);
    }

    @Override
    public void cleanupStage(StagedBinary staged) {
        if (staged == null || staged.path() == null)
            return;
        try {
            Path path = staged.path().toAbsolutePath().normalize();
            if (path.startsWith(stagingRoot))
                Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup; never mask the business outcome
        }
    }

    @Override
    public void deleteFinal(String storageKey) {
        try {
            Files.deleteIfExists(resolveStorageKey(storageKey));
        } catch (IOException ignored) {
            // rollback compensation is best effort; orphan remains private and
            // deterministic retry can reconcile it
        }
    }

    private void verify(Path path, long expectedSize, String expectedSha256) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != expectedSize) {
            throw new IOException("Evidence binary size/integrity mismatch.");
        }
        MessageDigest digest = sha256();
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0)
                    digest.update(buffer, 0, read);
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equals(expectedSha256))
            throw new IOException("Evidence binary hash/integrity mismatch.");
    }

    private Path requireStage(StagedBinary staged) throws IOException {
        if (staged == null || staged.path() == null)
            throw new IOException("Staging file không tồn tại.");
        Path path = staged.path().toAbsolutePath().normalize();
        if (!path.startsWith(stagingRoot) || !Files.isRegularFile(path))
            throw new IOException("Staging path không hợp lệ.");
        return path;
    }

    private Path resolveStorageKey(String storageKey) throws IOException {
        if (storageKey == null || !storageKey.matches("[0-9a-f]{2}/[0-9a-f]{64}")) {
            throw new IOException("Storage key không hợp lệ.");
        }
        Path path = objectRoot.resolve(storageKey).normalize();
        requireUnderRoot(path);
        return path;
    }

    private void requireNoSymlinkComponents(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        requireUnderRoot(absolute);
        Path relative = root.relativize(absolute);
        Path current = root;
        if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
            throw new IOException("Private evidence root không được là symbolic link.");
        }
        for (Path part : relative) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IOException("Evidence storage path chứa symbolic link.");
            }
        }
    }

    private void requireUnderRoot(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(root))
            throw new IllegalArgumentException("Storage path vượt private root.");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng.", ex);
        }
    }
}
