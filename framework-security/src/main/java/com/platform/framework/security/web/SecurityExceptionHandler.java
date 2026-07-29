package com.platform.framework.security.web;

import com.platform.framework.core.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security의 인가/인증 예외를 framework-core의 GlobalExceptionHandler보다 먼저 가로채서
 * ApiResponse 포맷 401/403으로 변환한다.
 *
 * ⚠️ 이 핸들러가 framework-security에 있는 이유: framework-core는 spring-security에 의존하지
 * 않으므로(계층 방향이 반대) AccessDeniedException 타입 자체를 알 수 없다. 실제로 겪은 버그:
 * @PreAuthorize 로 거부된 요청(AuthorizationDeniedException, AccessDeniedException의 하위 타입)이
 * GlobalExceptionHandler의 범용 @ExceptionHandler(Exception.class) 에 먼저 잡혀서 403 대신
 * 500(UnhandledException)으로 응답되었다. @Order(HIGHEST_PRECEDENCE) 로 이 어드바이스가
 * GlobalExceptionHandler보다 먼저 평가되도록 해서, 더 구체적인 타입인 AccessDeniedException을
 * 여기서 먼저 잡는다 — Spring은 여러 @ControllerAdvice 중 "먼저 매치되는" 것을 쓰지, 전체
 * 어드바이스를 통틀어 가장 구체적인 핸들러를 찾아주지 않는다.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("ERR-AUTH-0004", "접근 권한이 없습니다."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("ERR-AUTH-0003", "인증이 필요합니다."));
    }
}
