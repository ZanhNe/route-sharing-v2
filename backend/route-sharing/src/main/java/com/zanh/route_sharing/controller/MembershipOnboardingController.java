package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.membership.onboarding.*;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.security.OnboardingAuthenticationFilter;
import com.zanh.route_sharing.service.MembershipOnboardingService;
import com.zanh.route_sharing.service.membership.onboarding.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/onboarding/membership-profile")
@Validated
public class MembershipOnboardingController {
        private final MembershipOnboardingService service;

        public MembershipOnboardingController(MembershipOnboardingService service) {
                this.service = service;
        }

        @GetMapping
        @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_COMPLETE_PROFILE + "')")
        public ResponseEntity<ApiResponse<MembershipProfileResponse>> getCurrent(
                        @AuthenticationPrincipal CustomUserDetails principal) {
                return noStore(ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                                service.getCurrent(principal.getId()), "Lấy hồ sơ thành viên hiện tại thành công.")));
        }

        @PutMapping(path = "/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_COMPLETE_PROFILE + "')")
        public ResponseEntity<ApiResponse<MembershipProfileResponse>> saveDraft(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @RequestPart("profile") @Valid MembershipProfileDraftRequest request,
                        @RequestPart(value = "studentCardFront", required = false) MultipartFile studentCardFront,
                        @RequestPart(value = "studentCardBack", required = false) MultipartFile studentCardBack,
                        @RequestPart(value = "officialStudentConfirmation", required = false) MultipartFile officialStudentConfirmation) {
                MembershipDraftCommitResult result = service.saveDraft(principal.getId(), request,
                                studentCardFront, studentCardBack, officialStudentConfirmation);
                HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
                ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
                if (result.created())
                        builder.header(HttpHeaders.LOCATION, "/api/v1/onboarding/membership-profile");
                return noStore(builder.body(ApiResponse.success(status.value(), result.response(),
                                result.created() ? "Tạo hồ sơ nháp thành công." : "Cập nhật hồ sơ nháp thành công.")));
        }

        @PostMapping(path = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_COMPLETE_PROFILE + "')")
        public ResponseEntity<ApiResponse<MembershipSubmissionResponse>> submit(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @RequestPart("profile") @Valid MembershipProfileDraftRequest request,
                        @RequestPart(value = "studentCardFront", required = false) MultipartFile studentCardFront,
                        @RequestPart(value = "studentCardBack", required = false) MultipartFile studentCardBack,
                        @RequestPart(value = "officialStudentConfirmation", required = false) MultipartFile officialStudentConfirmation) {
                MembershipSubmissionResponse data = service.submit(principal.getId(), request,
                                studentCardFront, studentCardBack, officialStudentConfirmation);
                HttpStatus status = data.created() ? HttpStatus.CREATED : HttpStatus.OK;
                ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
                if (data.created())
                        builder.header(HttpHeaders.LOCATION, "/api/v1/onboarding/membership-profile");
                return noStore(builder.body(ApiResponse.success(status.value(), data,
                                data.created() ? "Nộp hồ sơ thành viên thành công."
                                                : "Hồ sơ này đã được nộp trước đó.")));
        }

        @GetMapping("/evidence/{evidenceId}/content")
        @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_COMPLETE_PROFILE + "')")
        public ResponseEntity<org.springframework.core.io.Resource> downloadEvidence(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive(message = "evidenceId phải là số dương.") Long evidenceId) {
                MembershipEvidenceDownloadResult result = service.downloadEvidence(principal.getId(), evidenceId);
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

        private static <T> ResponseEntity<T> noStore(ResponseEntity<T> response) {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(response.getHeaders());
                headers.setCacheControl("no-store");
                headers.setPragma("no-cache");
                return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
}
