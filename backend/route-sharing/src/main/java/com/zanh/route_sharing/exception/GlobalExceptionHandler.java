package com.zanh.route_sharing.exception;

import com.zanh.route_sharing.dto.response.ApiErrorResponse;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler() {
        this(Clock.systemUTC());
    }

    @Autowired
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> business(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage());
        String reference = ex instanceof SecurityAlertException security ? security.getReferenceCode() : null;
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request, ex.getErrors(), reference);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request, null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String key = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.putIfAbsent(key, error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ.",
                request, errors, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> constraintValidation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ.",
                request, errors, null);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<ApiErrorResponse> malformedRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Dữ liệu gửi lên không đúng định dạng.",
                request, null, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> missingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PARAMETER",
                "Thiếu tham số bắt buộc: " + ex.getParameterName() + ".", request, null, null);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handlerMethodValidation(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = ex.isForReturnValue() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        }
        String code = ex.isForReturnValue() ? "RETURN_VALUE_VALIDATION_ERROR" : "VALIDATION_ERROR";
        String message = ex.isForReturnValue()
                ? "Dữ liệu phản hồi của hệ thống không hợp lệ."
                : "Dữ liệu đầu vào không hợp lệ.";
        if (ex.isForReturnValue()) {
            log.error("Controller return-value validation failed", ex);
        }
        return response(status, code, message, request, null, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Phương thức HTTP không được hỗ trợ cho tài nguyên này.", request, null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> mediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Kiểu nội dung của request không được hỗ trợ.", request, null, null);
    }

    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    ResponseEntity<ApiErrorResponse> resourceNotFound(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Không tìm thấy tài nguyên được yêu cầu.", request, null, null);
    }

    @ExceptionHandler({ BadCredentialsException.class })
    ResponseEntity<ApiErrorResponse> badCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                "Email hoặc mật khẩu không chính xác.", request, null, null);
    }

    @ExceptionHandler({ DisabledException.class, LockedException.class, AccountExpiredException.class,
            CredentialsExpiredException.class })
    ResponseEntity<ApiErrorResponse> inactiveAccount(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE",
                "Tài khoản hiện không được phép đăng nhập.", request, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> authentication(AuthenticationException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Không thể xác thực phiên đăng nhập.", request, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Bạn không có quyền thực hiện thao tác này.", request, null, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> optimisticLock(ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Dữ liệu đã thay đổi bởi một yêu cầu khác. Vui lòng tải lại và thử lại.", request, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database constraint violation: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Dữ liệu xung đột với ràng buộc hệ thống.", request, null, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> uploadTooLarge(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return response(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
                "Tệp tải lên vượt quá dung lượng cho phép.", request, null, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unhandled(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Lỗi hệ thống. Vui lòng thử lại sau.", request, null, null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> errors,
            String referenceCode) {
        ApiErrorResponse body = new ApiErrorResponse(
                TimePolicy.now(clock), status.value(), code, message, request.getRequestURI(), errors, referenceCode);
        return ResponseEntity.status(status).body(body);
    }
}
