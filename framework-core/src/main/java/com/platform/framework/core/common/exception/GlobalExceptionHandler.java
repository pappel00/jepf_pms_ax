package com.platform.framework.core.common.exception;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.framework.core.common.response.ErrorDetail;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * MAD 4600. Exception 규격: GlobalExceptionHandler 에서 일괄 처리.
 * 모든 예외를 {@link ApiResponse} 포맷으로 변환하여 응답한다.
 *
 * @Order(LOWEST_PRECEDENCE) 로 명시적으로 가장 낮은 우선순위를 준다 — 다른 모듈(예: framework-security의
 * SecurityExceptionHandler)이 자기 도메인의 예외 타입을 더 구체적으로 먼저 처리할 수 있도록,
 * 이 클래스의 범용 @ExceptionHandler(Exception.class) 는 항상 맨 마지막 안전망 역할만 한다.
 * Spring은 여러 @ControllerAdvice 중 "먼저 매치되는" 것을 쓰지, 전체를 통틀어 가장 구체적인
 * 핸들러를 찾아주지 않기 때문에 순서 보장이 필요하다.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenant(TenantException e) {
        log.warn("[TenantException] {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityException e) {
        log.warn("[SecurityException] {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("[BusinessException] {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(InvalidRequestException e) {
        log.warn("[InvalidRequestException] {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage(), e.getErrors()));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApi(ExternalApiException e) {
        log.error("[ExternalApiException] {} - {}", e.getErrorCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST, "입력값 검증에 실패했습니다.", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<ErrorDetail> errors = e.getConstraintViolations().stream()
                .map(v -> new ErrorDetail(v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST, "입력값 검증에 실패했습니다.", errors));
    }

    /**
     * 요청 본문을 DTO로 파싱하는 단계 자체에서 실패한 경우 (JSON 문법 오류, primitive 타입 필드에
     * null 바인딩 시도 등). @Valid 검증(MethodArgumentNotValidException)보다 앞선 단계에서 터지는
     * 예외라 별도로 잡아줘야 한다 — 안 잡으면 밑의 범용 Exception.class 핸들러로 떨어져 500 +
     * 원인 불명확한 응답이 나간다 (실제로 겪은 버그: Menu 수정 시 sortOrder를 안 보내서 발생).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(CommonErrorCode.INVALID_REQUEST,
                        "요청 본문을 읽을 수 없습니다. 필수 필드가 빠졌거나 JSON 형식이 올바르지 않은지 확인하세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다."));
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return new ErrorDetail(fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage());
    }
}
