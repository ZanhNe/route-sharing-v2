package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.complaint.evidence.*;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.ComplaintEvidenceService;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import com.zanh.route_sharing.utils.PaginationPolicy;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/complaints")
@Validated
public class ComplaintEvidenceController {
    private final ComplaintEvidenceService service;

    public ComplaintEvidenceController(ComplaintEvidenceService service) {
        this.service = service;
    }

    @PostMapping(path = "/{complaintId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADD_OWN_COMPLAINT_EVIDENCE')")
    public ResponseEntity<ApiResponse<EvidenceUploadResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "complaintId phải là số dương.") Long complaintId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description) {
        EvidenceUploadResponse data = service.upload(principal.getId(), complaintId, file, description);
        HttpStatus status = data.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = data.created() ? "Tải bằng chứng lên thành công."
                : "Bằng chứng này đã được ghi nhận trước đó.";
        return ResponseEntity.status(status).body(ApiResponse.success(status.value(), data, message));
    }

    @GetMapping("/{complaintId}/evidence")
    @PreAuthorize("hasAuthority('VIEW_OWN_COMPLAINT_EVIDENCE')")
    public ResponseEntity<ApiResponse<EvidencePageResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "complaintId phải là số dương.") Long complaintId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1.") @Max(value = PaginationPolicy.MAX_SIZE, message = "size không được vượt quá 50.") int size) {
        EvidencePageResult result = service.listOwn(principal.getId(), complaintId, page, size);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), result.data(),
                "Lấy danh sách bằng chứng thành công.", result.meta()));
    }

    @GetMapping("/{complaintId}/evidence/{evidenceId}/content")
    @PreAuthorize("hasAuthority('VIEW_OWN_COMPLAINT_EVIDENCE')")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "complaintId phải là số dương.") Long complaintId,
            @PathVariable @Positive(message = "evidenceId phải là số dương.") Long evidenceId) {
        EvidenceDownloadResult result = service.downloadOwn(principal.getId(), complaintId, evidenceId);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(result.verifiedMediaType());
        } catch (IllegalArgumentException ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(result.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(result.resource());
    }
}
