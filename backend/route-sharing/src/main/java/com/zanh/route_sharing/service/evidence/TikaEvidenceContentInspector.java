package com.zanh.route_sharing.service.evidence;

import com.zanh.route_sharing.exception.BusinessException;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipFile;

@Component
public class TikaEvidenceContentInspector implements EvidenceContentInspector {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final Tika tika = new Tika();

    @Override
    public EvidenceInspection inspect(Path stagedPath) {
        if (stagedPath == null)
            throw invalidContent();
        try {
            String mediaType;
            try (InputStream input = java.nio.file.Files.newInputStream(stagedPath)) {
                mediaType = tika.detect(input).toLowerCase(Locale.ROOT);
            }
            if (isGenericZip(mediaType) && isDocxContainer(stagedPath)) {
                mediaType = DOCX;
            }
            if (mediaType.isBlank())
                throw invalidContent();
            return new EvidenceInspection(mediaType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalidContent();
        }
    }

    private static boolean isGenericZip(String mediaType) {
        return "application/zip".equals(mediaType)
                || "application/x-tika-ooxml".equals(mediaType)
                || "application/x-zip-compressed".equals(mediaType);
    }

    private static boolean isDocxContainer(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            return zip.getEntry("[Content_Types].xml") != null
                    && zip.getEntry("_rels/.rels") != null
                    && zip.getEntry("word/document.xml") != null;
        }
    }

    private static BusinessException invalidContent() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "EVIDENCE_CONTENT_INVALID",
                "Không thể xác minh nội dung tệp bằng chứng.");
    }
}
