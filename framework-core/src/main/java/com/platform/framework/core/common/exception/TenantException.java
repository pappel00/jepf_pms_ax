package com.platform.framework.core.common.exception;

/** 테넌트 관련 예외: 만료된 회사, 존재하지 않는 회사 등 (ERR-TENANT-XXXX). */
public class TenantException extends BaseException {

    public TenantException(String errorCode, String message) {
        super(errorCode, message);
    }

    public TenantException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
