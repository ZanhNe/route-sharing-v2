package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationRequest;
import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationLegalContextResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationSchoolResponse;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.service.AccountRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AccountRegistrationController {
    private static final String UNKNOWN_USER_AGENT = "UNKNOWN";
    private static final String UNKNOWN_REMOTE_ADDRESS = "unknown";

    private final AccountRegistrationService service;

    public AccountRegistrationController(AccountRegistrationService service) {
        this.service = service;
    }

    @GetMapping("/registration/schools")
    public ResponseEntity<ApiResponse<List<RegistrationSchoolResponse>>> listRegistrationSchools() {
        List<RegistrationSchoolResponse> data = service.listRegistrationSchools();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data,
                "Lấy danh sách trường có thể đăng ký thành công."));
    }

    @GetMapping("/registration/schools/{schoolId}/legal-documents")
    public ResponseEntity<ApiResponse<RegistrationLegalContextResponse>> getRegistrationLegalContext(
            @PathVariable @Positive Long schoolId) {
        RegistrationLegalContextResponse data = service.getRegistrationLegalContext(schoolId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data,
                "Lấy văn bản pháp lý đăng ký thành công."));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountRegistrationResponse>> register(
            @Valid @RequestBody AccountRegistrationRequest request,
            HttpServletRequest servletRequest) {
        AccountRegistrationResponse data = service.register(
                request,
                boundedRemoteAddress(servletRequest.getRemoteAddr()),
                boundedUserAgent(servletRequest.getHeader("User-Agent")));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), data, "Đăng ký tài khoản thành công."));
    }

    private static String boundedRemoteAddress(String value) {
        String normalized = value == null || value.isBlank() ? UNKNOWN_REMOTE_ADDRESS : value.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private static String boundedUserAgent(String value) {
        String normalized = value == null || value.isBlank() ? UNKNOWN_USER_AGENT : value.trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
