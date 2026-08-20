package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.complaint.evidence.*;
import com.zanh.route_sharing.dto.complaint.review.ComplaintReviewerEvidencePageResponse;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.ComplaintReviewEvidenceService;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import com.zanh.route_sharing.utils.PaginationPolicy;
import jakarta.servlet.http.HttpServletRequest;
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
public class ComplaintReviewEvidenceController {
    private final ComplaintReviewEvidenceService service;

    public ComplaintReviewEvidenceController(ComplaintReviewEvidenceService service) {
        this.service = service;
    }

    @PostMapping(path = "/{complaintId}/review-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PARTICIPATE_IN_OWN_COMPLAINT_REVIEW')")
    public ResponseEntity<ApiResponse<EvidenceUploadResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long complaintId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description) {
        EvidenceUploadResponse data = service.upload(principal.getId(), complaintId, file, description);
        int status = data.created() ? 201 : 200;
        return ResponseEntity.status(status).body(ApiResponse.success(status, data,
                data.created() ? "Tải review evidence thành công." : "Review evidence này đã được ghi nhận trước đó."));
    }

    @GetMapping("/{complaintId}/review-evidence")
    @PreAuthorize("hasAuthority('PARTICIPATE_IN_OWN_COMPLAINT_REVIEW')")
    public ResponseEntity<ApiResponse<EvidencePageResponse>> listOwn(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long complaintId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(PaginationPolicy.MAX_SIZE) int size) {
        EvidencePageResult result = service.listOwn(principal.getId(), complaintId, page, size);
        return ResponseEntity
                .ok(ApiResponse.of(200, result.data(), "Lấy review evidence của bạn thành công.", result.meta()));
    }

    @GetMapping("/{complaintId}/review-evidence/{evidenceId}/content")
    @PreAuthorize("hasAuthority('PARTICIPATE_IN_OWN_COMPLAINT_REVIEW')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadOwn(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long complaintId,
            @PathVariable @Positive Long evidenceId) {
        return binary(service.downloadOwn(principal.getId(), complaintId, evidenceId));
    }

    @GetMapping("/{complaintId}/reviewer-evidence")
    @PreAuthorize("hasAuthority('HANDLE_COMPLAINT') and hasAuthority('VIEW_COMPLAINT_SENSITIVE_EVIDENCE')")
    public ResponseEntity<ApiResponse<ComplaintReviewerEvidencePageResponse>> listReviewer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long complaintId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(PaginationPolicy.MAX_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success(200,
                service.listReviewer(principal.getId(), complaintId, page, size),
                "Lấy metadata evidence phục vụ review thành công."));
    }

    @GetMapping("/{complaintId}/reviewer-evidence/{evidenceId}/content")
    @PreAuthorize("hasAuthority('HANDLE_COMPLAINT') and hasAuthority('VIEW_COMPLAINT_SENSITIVE_EVIDENCE')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadReviewer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long complaintId,
            @PathVariable @Positive Long evidenceId,
            HttpServletRequest request) {
        return binary(service.downloadReviewer(principal.getId(), complaintId, evidenceId,
                clientIp(request), request.getHeader("User-Agent")));
    }

    private static ResponseEntity<org.springframework.core.io.Resource> binary(EvidenceDownloadResult result) {
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(result.verifiedMediaType());
        } catch (IllegalArgumentException ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(contentType).contentLength(result.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff").body(result.resource());
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
