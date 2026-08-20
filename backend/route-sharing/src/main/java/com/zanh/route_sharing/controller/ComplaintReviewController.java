package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.complaint.review.*;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.ComplaintReviewService;
import com.zanh.route_sharing.utils.PaginationPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/complaints")
@Validated
public class ComplaintReviewController {
        private final ComplaintReviewService service;

        public ComplaintReviewController(ComplaintReviewService service) {
                this.service = service;
        }

        @GetMapping("/review-queue")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewQueueResponse>> queue(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @RequestParam(defaultValue = "SUBMITTED") String status,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "10") @Min(1) @Max(PaginationPolicy.MAX_SIZE) int size) {
                return ResponseEntity.ok(ApiResponse.success(200, service.queue(principal.getId(), status, page, size),
                                "Lấy hàng đợi xử lý khiếu nại thành công."));
        }

        @GetMapping("/{complaintId}/review-case")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewCaseResponse>> reviewerCase(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId) {
                return ResponseEntity.ok(ApiResponse.success(200, service.reviewerCase(principal.getId(), complaintId),
                                "Lấy hồ sơ xử lý khiếu nại thành công."));
        }

        @PostMapping("/{complaintId}/claim")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewActionResponse>> claim(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId) {
                return ResponseEntity.ok(ApiResponse.success(200, service.claim(principal.getId(), complaintId),
                                "Tiếp nhận xử lý khiếu nại thành công."));
        }

        @GetMapping("/{complaintId}/eligible-reviewers")
        @PreAuthorize("hasAuthority('REASSIGN_COMPLAINT')")
        public ResponseEntity<ApiResponse<EligibleReviewerPageResponse>> eligibleReviewers(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "10") @Min(1) @Max(PaginationPolicy.MAX_SIZE) int size) {
                return ResponseEntity.ok(ApiResponse.success(200,
                                service.eligibleReviewers(principal.getId(), complaintId, page, size),
                                "Lấy danh sách reviewer phù hợp thành công."));
        }

        @PostMapping("/{complaintId}/reassign")
        @PreAuthorize("hasAuthority('REASSIGN_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewActionResponse>> reassign(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @Valid @RequestBody ComplaintReassignRequest request) {
                return ResponseEntity
                                .ok(ApiResponse.success(200, service.reassign(principal.getId(), complaintId, request),
                                                "Chuyển reviewer khiếu nại thành công."));
        }

        @GetMapping("/{complaintId}/review")
        @PreAuthorize("hasAuthority('PARTICIPATE_IN_OWN_COMPLAINT_REVIEW')")
        public ResponseEntity<ApiResponse<ComplaintParticipantReviewResponse>> participantView(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId) {
                return ResponseEntity
                                .ok(ApiResponse.success(200, service.participantView(principal.getId(), complaintId),
                                                "Lấy trạng thái xử lý khiếu nại thành công."));
        }

        @PostMapping("/{complaintId}/response")
        @PreAuthorize("hasAuthority('PARTICIPATE_IN_OWN_COMPLAINT_REVIEW')")
        public ResponseEntity<ApiResponse<ComplaintFormalResponseResponse>> respond(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @Valid @RequestBody ComplaintResponseRequest request) {
                var data = service.respond(principal.getId(), complaintId, request);
                int status = data.created() ? 201 : 200;
                String message = data.created() ? "Gửi phản hồi khiếu nại thành công."
                                : "Phản hồi này đã được ghi nhận trước đó.";
                return ResponseEntity.status(status).body(ApiResponse.success(status, data, message));
        }

        @PostMapping("/{complaintId}/evidence-request")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewActionResponse>> requestMoreEvidence(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @Valid @RequestBody ComplaintEvidenceRequest request) {
                return ResponseEntity.ok(ApiResponse.success(200,
                                service.requestMoreEvidence(principal.getId(), complaintId, request),
                                "Yêu cầu bổ sung bằng chứng thành công."));
        }

        @PostMapping("/{complaintId}/resume-review")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewActionResponse>> resume(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId) {
                return ResponseEntity.ok(ApiResponse.success(200, service.resume(principal.getId(), complaintId),
                                "Tiếp tục xử lý khiếu nại thành công."));
        }

        @PostMapping("/{complaintId}/review-decision")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT')")
        public ResponseEntity<ApiResponse<ComplaintReviewActionResponse>> finalizeReview(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @Valid @RequestBody ComplaintReviewDecisionRequest request) {
                return ResponseEntity.ok(ApiResponse.success(200,
                                service.finalizeReview(principal.getId(), complaintId, request),
                                "Kết luận xử lý khiếu nại thành công."));
        }

        @GetMapping("/{complaintId}/investigation-context")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT') and hasAuthority('VIEW_COMPLAINT_SENSITIVE_EVIDENCE')")
        public ResponseEntity<ApiResponse<ComplaintInvestigationContextResponse>> investigation(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        HttpServletRequest request) {
                return ResponseEntity.ok(ApiResponse.success(200,
                                service.investigation(principal.getId(), complaintId, clientIp(request),
                                                userAgent(request)),
                                "Lấy dữ liệu đối chiếu khiếu nại thành công."));
        }

        @GetMapping("/{complaintId}/location-evidence")
        @PreAuthorize("hasAuthority('HANDLE_COMPLAINT') and hasAuthority('VIEW_COMPLAINT_SENSITIVE_EVIDENCE')")
        public ResponseEntity<ApiResponse<ComplaintLocationEvidencePageResponse>> locations(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive Long complaintId,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
                        HttpServletRequest request) {
                return ResponseEntity.ok(ApiResponse.success(200,
                                service.locations(principal.getId(), complaintId, page, size, clientIp(request),
                                                userAgent(request)),
                                "Lấy lịch sử vị trí phục vụ xử lý khiếu nại thành công."));
        }

        private static String clientIp(HttpServletRequest request) {
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank())
                        return forwarded.split(",")[0].trim();
                return request.getRemoteAddr();
        }

        private static String userAgent(HttpServletRequest request) {
                return request.getHeader("User-Agent");
        }
}
