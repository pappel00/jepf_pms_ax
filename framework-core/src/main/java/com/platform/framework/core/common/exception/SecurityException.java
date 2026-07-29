package com.platform.framework.core.common.exception;

/**
 * 보안 관련 예외: JWT 만료, 권한 부족 등 (ERR-AUTH-XXXX).
 * java.lang.SecurityException 과 이름이 같지만, 같은 패키지에 선언된 타입이
 * java.lang 의 타입보다 우선 적용되므로 정상적으로 컴파일된다.
 */
public class SecurityException extends BaseException {

    public SecurityException(String errorCode, String message) {
        super(errorCode, message);
    }

    public SecurityException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
