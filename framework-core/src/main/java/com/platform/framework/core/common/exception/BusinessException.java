package com.platform.framework.core.common.exception;

/** 비즈니스 규칙 위반 예외: 잔액 부족, 중복 아이디 등. */
public class BusinessException extends BaseException {

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
