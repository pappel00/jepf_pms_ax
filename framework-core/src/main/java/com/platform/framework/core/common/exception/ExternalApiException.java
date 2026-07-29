package com.platform.framework.core.common.exception;

/** 외부 연동 에러: 파일 스토리지 장애, 외부 API 통신 실패 등. */
public class ExternalApiException extends BaseException {

    public ExternalApiException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ExternalApiException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
