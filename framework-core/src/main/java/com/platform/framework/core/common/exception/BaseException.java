package com.platform.framework.core.common.exception;

/**
 * MAD 4600. Exception 규격의 최상위 공통 예외.
 * java.lang.RuntimeException 기반이며, 모든 프레임워크 예외는 이 클래스를 상속한다.
 * errorCode는 "ERR-[모듈]-[네자리숫자]" 규칙을 따른다.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    protected BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
