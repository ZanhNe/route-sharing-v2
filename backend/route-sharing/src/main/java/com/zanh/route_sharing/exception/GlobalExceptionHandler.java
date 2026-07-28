package com.zanh.route_sharing.exception;

import com.zanh.route_sharing.dto.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Lỗi validation dữ liệu
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("VALIDATION_ERROR: Dữ liệu đầu vào không hợp lệ");

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Dữ liệu đầu vào không hợp lệ")
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Lỗi không tìm thấy
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("RESOURCE_NOT_FOUND: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Lỗi nghiệp vụ
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("BUSINESS_ERROR: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(ex.getStatus().value())
                .message(ex.getMessage())
                .errors(ex.getErrors())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // An ninh
    @ExceptionHandler(SecurityAlertException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurityAlertException(SecurityAlertException ex) {
        log.error("CRITICAL_SECURITY_ALERT: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.LOCKED.value()) // HTTP 423
                .message(ex.getMessage())
                .errors(Map.of("security", "Chuyến đi bị khóa. Ứng dụng bị đình chỉ để An ninh xác minh."))
                .meta(ex.getMetaInfo()) // Ném cái Hộp đen/Waypoints bị đóng băng vào đây
                .build();

        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    // Lỗi phân quyền
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("ACCESS_DENIED: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Bạn không có quyền thực hiện thao tác này.")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Lỗi runtime
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(Exception ex) {
        log.error("INTERNAL_SERVER_ERROR: ", ex); // log track debug

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Lỗi hệ thống, vui lòng thử lại sau ít phút")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}